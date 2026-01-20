// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Jest tests focused on removal of hard-coded password and secure token handling.

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
    let originalWebgoat;
    let ajaxSpy;

    beforeEach(() => {
        originalWebgoat = global.webgoat;
        global.webgoat = {
            config: {
                getJwtDemoPassword: jest.fn().mockReturnValue('secure-config-password')
            },
            customjs: {}
        };

        ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
            success: function (cb) {
                cb({
                    access_token: 'access-token-from-server',
                    refresh_token: 'refresh-token-from-server'
                });
            }
        }));

        global.localStorage = (function () {
            let store = {};
            return {
                getItem: key => store[key] || null,
                setItem: (key, value) => { store[key] = String(value); },
                clear: () => { store = {}; }
            };
        })();

        jest.resetModules();
    });

    afterEach(() => {
        ajaxSpy.mockRestore();
        global.webgoat = originalWebgoat;
    });

    test('login uses configured password instead of hard-coded literal', () => {
        const script = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        const user = 'Jerry';
        script.login(user);

        expect(global.webgoat.config.getJwtDemoPassword).toHaveBeenCalled();

        expect(ajaxSpy).toHaveBeenCalledTimes(1);
        const callArgs = ajaxSpy.mock.calls[0][0];
        const body = JSON.parse(callArgs.data);

        expect(body.user).toBe(user);
        expect(body.password).toBe('secure-config-password');
        expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
    });

    test('newToken updates tokens from server response, not from undefined variables', () => {
        const script = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        localStorage.setItem('access_token', 'existing-access');
        localStorage.setItem('refresh_token', 'existing-refresh');

        ajaxSpy.mockImplementation(() => ({
            success: function (cb) {
                cb({
                    access_token: 'new-access-token',
                    refresh_token: 'new-refresh-token'
                });
            }
        }));

        script.newToken();

        expect(localStorage.getItem('access_token')).toBe('new-access-token');
        expect(localStorage.getItem('refresh_token')).toBe('new-refresh-token');
    });
});
