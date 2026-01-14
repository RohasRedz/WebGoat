/* eslint-env jest */
/* global webgoat */

describe('jwt-refresh.js delta tests', () => {
    beforeEach(() => {
        global.localStorage = (function () {
            let store = {};
            return {
                getItem: (key) => store[key] || null,
                setItem: (key, value) => {
                    store[key] = String(value);
                },
                clear: () => {
                    store = {};
                }
            };
        })();

        global.webgoat = {
            config: {
                getJwtClientSecret: jest.fn().mockReturnValue('runtime-secret')
            },
            customjs: {}
        };

        global.$ = jest.fn().mockImplementation(() => ({}));
        $.ajax = jest.fn();
    });

    afterEach(() => {
        jest.resetAllMocks();
        delete global.webgoat;
        delete global.$;
        delete global.localStorage;
    });

    function loadModule() {
        jest.resetModules();
        return require('../../../../../../test/resources/lessons/jwt/js/jwt-refresh.js');
    }

    test('login() must not use hard-coded password and should call getJwtPassword()', () => {
        loadModule();

        const ajaxSpy = $.ajax;

        const expectedBody = { user: 'Jerry', password: 'runtime-secret' };

        require('../../../../../../test/resources/lessons/jwt/js/jwt-refresh.js');

        expect(webgoat.config.getJwtClientSecret).toHaveBeenCalled();

        const ajaxCall = ajaxSpy.mock.calls.find(
            (call) => call[0] && call[0].url === 'JWT/refresh/login'
        );
        expect(ajaxCall).toBeDefined();

        const dataSent = JSON.parse(ajaxCall[0].data);
        expect(dataSent).toEqual(expectedBody);
        expect(dataSent.password).toBe('runtime-secret');
    });

    test('newToken() should update tokens from server response instead of hard-coded variables', () => {
        loadModule();

        localStorage.setItem('access_token', 'old-access');
        localStorage.setItem('refresh_token', 'old-refresh');

        $.ajax.mockImplementation((options) => {
            const success = options.success || options.success;
            if (success) {
                success({
                    access_token: 'new-access',
                    refresh_token: 'new-refresh'
                });
            }
            return {
                success: (cb) => {
                    cb({
                        access_token: 'new-access',
                        refresh_token: 'new-refresh'
                    });
                    return this;
                }
            };
        });

        const module = require('../../../../../../test/resources/lessons/jwt/js/jwt-refresh.js');

        module.newToken();

        expect(localStorage.getItem('access_token')).toBe('new-access');
        expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
    });
});
