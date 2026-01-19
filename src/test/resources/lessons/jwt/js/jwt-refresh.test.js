// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/* eslint-env jest */

// NOTE: We assume jwt-refresh.js is loaded as a plain script in tests.
// To keep imports aligned with the updated file, we rely on the global jQuery ($) and webgoat.customjs.

/* global $, webgoat, WEBGOAT_DEMO_JWT_PASSWORD */

describe('jwt-refresh delta tests - credential handling', () => {
    let originalAjax;
    let originalWebgoat;
    let originalPasswordConst;

    beforeEach(() => {
        // Mock jQuery.ajax
        originalAjax = $.ajax;
        $.ajax = jest.fn().mockReturnValue({
            success: function (cb) {
                cb({ access_token: 'access', refresh_token: 'refresh' });
                return this;
            }
        });

        // Ensure localStorage is available
        global.localStorage = (function () {
            let store = {};
            return {
                getItem: key => store[key],
                setItem: (key, value) => { store[key] = value; },
                clear: () => { store = {}; }
            };
        })();

        // Mock webgoat.customjs if not present
        originalWebgoat = global.webgoat;
        global.webgoat = global.webgoat || {};
        global.webgoat.customjs = {};
        originalPasswordConst = global.WEBGOAT_DEMO_JWT_PASSWORD;
    });

    afterEach(() => {
        $.ajax = originalAjax;
        global.webgoat = originalWebgoat;
        global.WEBGOAT_DEMO_JWT_PASSWORD = originalPasswordConst;
        if (global.localStorage && global.localStorage.clear) {
            global.localStorage.clear();
        }
    });

    test('login uses WEBGOAT_DEMO_JWT_PASSWORD constant instead of hard-coded literal', () => {
        // Arrange: override the password constant to a known value
        global.WEBGOAT_DEMO_JWT_PASSWORD = 'override-demo-password';
        // Require the script after overriding the constant so it picks up our value
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        // Act: call login from the loaded script
        // login is in the global scope in the original file
        global.login('Jerry');

        // Assert: ajax was called with our override password value,
        // proving that the constant is used instead of an inline hard-coded literal.
        expect($.ajax).toHaveBeenCalledTimes(1);
        const call = $.ajax.mock.calls[0][0];
        const payload = JSON.parse(call.data);
        expect(payload.password).toBe('override-demo-password');
    });

    test('addBearerToken builds Authorization header from stored access token', () => {
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
        localStorage.setItem('access_token', 'xyz');

        const headers = webgoat.customjs.addBearerToken();
        expect(headers.Authorization).toBe('Bearer xyz');
    });
});
