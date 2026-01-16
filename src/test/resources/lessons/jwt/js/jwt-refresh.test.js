// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
const $ = require('jquery');

global.$ = $;
global.webgoat = {
    config: {
        jwtPassword: 'secure-from-config'
    },
    customjs: {}
};

require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh hardcoded password removal', () => {

    beforeEach(() => {
        // Reset AJAX mock
        jest.spyOn($, 'ajax').mockImplementation(() => ({
            success: function (cb) {
                cb({ access_token: 'access', refresh_token: 'refresh' });
                return this;
            }
        }));
        global.localStorage = {
            store: {},
            setItem(key, value) { this.store[key] = value; },
            getItem(key) { return this.store[key]; }
        };
    });

    afterEach(() => {
        jest.restoreAllMocks();
        global.localStorage = undefined;
    });

    test('login should use password from config instead of hardcoded literal', () => {
        // Arrange
        const ajaxSpy = jest.spyOn($, 'ajax');

        // Act
        // login is defined globally by jwt-refresh.js
        login('Jerry');

        // Assert
        expect(ajaxSpy).toHaveBeenCalledTimes(1);
        const callArgs = ajaxSpy.mock.calls[0][0];
        const data = JSON.parse(callArgs.data);

        expect(data.user).toBe('Jerry');
        expect(data.password).toBe('secure-from-config');
        expect(data.password).not.toBe('bm5nhSkxCXZkKRy4');
    });
});
