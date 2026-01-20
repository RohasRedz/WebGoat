// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/**
 * Delta tests for jwt-refresh.js focusing on:
 * - login() using getConfiguredPassword() instead of a hard-coded literal.
 * - Behavior when WEBGOAT_JWT_DEMO_PASSWORD is defined vs undefined.
 */

const $ = require('jquery');

// Since the original script uses global jQuery and DOM ready, we simulate the environment.
describe('jwt-refresh delta tests', () => {
    let originalPasswordGlobal;
    let originalLocalStorage;

    beforeEach(() => {
        // mock global WEBGOAT_JWT_DEMO_PASSWORD if present
        originalPasswordGlobal = global.WEBGOAT_JWT_DEMO_PASSWORD;
        delete global.WEBGOAT_JWT_DEMO_PASSWORD;

        // mock localStorage
        originalLocalStorage = global.localStorage;
        const store = {};
        global.localStorage = {
            getItem: (k) => store[k],
            setItem: (k, v) => { store[k] = v; }
        };

        // mock $.ajax
        jest.spyOn($, 'ajax').mockImplementation((options) => {
            // mimic jQuery's success callback signature
            if (options && typeof options.success === 'function') {
                options.success({
                    access_token: 'ACCESS',
                    refresh_token: 'REFRESH'
                });
            }
            return { success: (cb) => cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' }) };
        });

        global.$ = $;
        global.webgoat = { customjs: {} };

        // Require module under test after globals are set
        jest.isolateModules(() => {
            require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
        });
    });

    afterEach(() => {
        if (originalPasswordGlobal !== undefined) {
            global.WEBGOAT_JWT_DEMO_PASSWORD = originalPasswordGlobal;
        } else {
            delete global.WEBGOAT_JWT_DEMO_PASSWORD;
        }
        global.localStorage = originalLocalStorage;
        jest.restoreAllMocks();
        delete require.cache[require.resolve('../../../../main/resources/lessons/jwt/js/jwt-refresh.js')];
    });

    test('login uses configured password when WEBGOAT_JWT_DEMO_PASSWORD is set', () => {
        // Arrange
        global.WEBGOAT_JWT_DEMO_PASSWORD = 'configured-secret';
        const ajaxSpy = jest.spyOn($, 'ajax');

        // Act
        // call login directly from required module's global scope
        global.login('Jerry');

        // Assert
        expect(ajaxSpy).toHaveBeenCalledTimes(1);
        const callArgs = ajaxSpy.mock.calls[0][0];
        const payload = JSON.parse(callArgs.data);
        expect(payload.user).toBe('Jerry');
        expect(payload.password).toBe('configured-secret');
    });

    test('login falls back to empty password when WEBGOAT_JWT_DEMO_PASSWORD is not set', () => {
        // Arrange
        delete global.WEBGOAT_JWT_DEMO_PASSWORD;
        const ajaxSpy = jest.spyOn($, 'ajax');

        // Act
        global.login('Jerry');

        // Assert
        const callArgs = ajaxSpy.mock.calls[0][0];
        const payload = JSON.parse(callArgs.data);
        expect(payload.user).toBe('Jerry');
        expect(payload.password).toBe('');
    });
});
