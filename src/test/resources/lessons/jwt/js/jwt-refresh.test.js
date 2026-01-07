jest.mock('jquery', () => {
    const original = jest.requireActual('jquery');
    const $ = (...args) => original(...args);
    $.ajax = jest.fn().mockReturnValue({
        success: function (cb) {
            cb({
                access_token: 'access-token',
                refresh_token: 'refresh-token'
            });
            return this;
        }
    });
    return $;
});

const $ = require('jquery');

require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh login (delta tests for hard-coded password removal)', () => {
    let originalPassword;

    beforeEach(() => {
        $.ajax.mockClear();
        global.localStorage = (function () {
            let store = {};
            return {
                getItem: (key) => store[key],
                setItem: (key, value) => {
                    store[key] = value;
                },
                clear: () => {
                    store = {};
                }
            };
        })();
        originalPassword = global.WEBGOAT_JWT_PASSWORD;
        delete global.WEBGOAT_JWT_PASSWORD;
    });

    afterEach(() => {
        if (originalPassword !== undefined) {
            global.WEBGOAT_JWT_PASSWORD = originalPassword;
        } else {
            delete global.WEBGOAT_JWT_PASSWORD;
        }
        if (global.localStorage && global.localStorage.clear) {
            global.localStorage.clear();
        }
    });

    it('does not send a login request when WEBGOAT_JWT_PASSWORD is missing', () => {
        global.login('Jerry');

        expect($.ajax).not.toHaveBeenCalled();
    });

    it('sends a login request with the configured password when WEBGOAT_JWT_PASSWORD is defined', () => {
        global.WEBGOAT_JWT_PASSWORD = 'secure-runtime-password';

        global.login('Jerry');

        expect($.ajax).toHaveBeenCalledTimes(1);
        const call = $.ajax.mock.calls[0][0];
        expect(call.type).toBe('POST');
        expect(call.url).toBe('JWT/refresh/login');
        expect(call.contentType).toBe('application/json');
        const body = JSON.parse(call.data);
        expect(body.user).toBe('Jerry');
        expect(body.password).toBe('secure-runtime-password');

        expect(global.localStorage.getItem('access_token')).toBe('access-token');
        expect(global.localStorage.getItem('refresh_token')).toBe('refresh-token');
    });
});
