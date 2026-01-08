// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta tests for jwt-refresh.js focusing on:
// - removal of hard-coded password literal
// - use of configurable/demo password
// - tokens are set only when present in the response.

const $ = require('jquery');

describe('jwt-refresh login and token handling (delta tests)', () => {
    let originalAjax;
    let ajaxMock;
    let jwtModule;

    beforeEach(() => {
        // Mock localStorage
        const store = {};
        global.localStorage = {
            getItem: (k) => store[k] || null,
            setItem: (k, v) => { store[k] = String(v); },
            removeItem: (k) => { delete store[k]; },
            clear: () => { Object.keys(store).forEach(k => delete store[k]); }
        };

        // Spy on $.ajax to control responses
        originalAjax = $.ajax;
        ajaxMock = jest.fn();
        $.ajax = ajaxMock;

        // Ensure global namespace for webgoat.customjs
        global.webgoat = global.webgoat || {};
        global.webgoat.customjs = {};

        // Clear any demo password override
        delete global.WEBGOAT_DEMO_PASSWORD;

        // Require module under test after mocks are in place
        jest.isolateModules(() => {
            jwtModule = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
        });

        // Clear any calls previously made by document.ready
        ajaxMock.mockClear();
    });

    afterEach(() => {
        $.ajax = originalAjax;
    });

    test('login uses configurable/demo password instead of hard-coded literal', () => {
        // Arrange a specific demo password and capture AJAX payload
        global.WEBGOAT_DEMO_PASSWORD = 'test-demo-password';

        // Mock AJAX success chain
        ajaxMock.mockImplementation((options) => {
            const success = options.success || options.successCallback || options.successFn;
            if (typeof success === 'function') {
                success({ access_token: 'a', refresh_token: 'r' });
            }
            // jQuery returns a jqXHR; for our purposes a plain object is enough
            return { done: () => {} };
        });

        // Call login directly with a known user
        const login = global.login || require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js').login;
        login('Jerry');

        expect(ajaxMock).toHaveBeenCalledTimes(1);
        const callArgs = ajaxMock.mock.calls[0][0];
        const payload = JSON.parse(callArgs.data);
        expect(payload.user).toBe('Jerry');
        expect(payload.password).toBe('test-demo-password');
    });

    test('tokens are stored only when present in the response', () => {
        // First, simulate a response with both tokens
        ajaxMock.mockImplementationOnce((options) => {
            if (typeof options.success === 'function') {
                options.success({ access_token: 'access123', refresh_token: 'refresh123' });
            }
            return { done: () => {} };
        });

        const login = global.login || require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js').login;
        login('Jerry');

        expect(localStorage.getItem('access_token')).toBe('access123');
        expect(localStorage.getItem('refresh_token')).toBe('refresh123');

        // Then, simulate a response missing tokens; they should not be overwritten or set
        ajaxMock.mockImplementationOnce((options) => {
            if (typeof options.success === 'function') {
                options.success({}); // no tokens present
            }
            return { done: () => {} };
        });

        login('Jerry');

        // Previous values should remain unchanged
        expect(localStorage.getItem('access_token')).toBe('access123');
        expect(localStorage.getItem('refresh_token')).toBe('refresh123');
    });

    test('addBearerToken only sets Authorization header when access_token exists', () => {
        // Initially, no token in storage
        localStorage.clear();

        const headersEmpty = global.webgoat.customjs.addBearerToken();
        expect(headersEmpty.Authorization).toBeUndefined();

        // After setting token
        localStorage.setItem('access_token', 'token-xyz');
        const headersWithToken = global.webgoat.customjs.addBearerToken();
        expect(headersWithToken.Authorization).toBe('Bearer token-xyz');
    });

    test('newToken aborts when no refresh_token is present', () => {
        localStorage.clear();

        const newTokenFn = global.newToken || require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js').newToken;
        newTokenFn();

        // Without refresh_token, no request should be fired
        expect(ajaxMock).not.toHaveBeenCalled();
    });
});
