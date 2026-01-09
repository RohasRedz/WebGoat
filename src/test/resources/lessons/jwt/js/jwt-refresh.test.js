// Assumed logical Jest test location for WebGoat JS resources:
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 *  - removal of hard-coded password
 *  - behavior of login(user, password) and newToken()
 */

jest.mock('jquery', () => {
    const ajaxMock = jest.fn(() => ({
        success: function (cb) {
            // For tests that need a success callback, they will call ajaxMock.mock.calls[x][0].success
            if (typeof cb === 'function') {
                // store for manual invocation
                ajaxMock._lastSuccess = cb;
            }
            return this;
        }
    }));
    ajaxMock._lastSuccess = null;
    return ajaxMock;
});

const $ = require('jquery');

describe('jwt-refresh.js - delta security tests', () => {
    beforeEach(() => {
        // Reset localStorage and jQuery ajax mock
        global.localStorage = (function () {
            let store = {};
            return {
                getItem: key => store[key] || null,
                setItem: (key, value) => { store[key] = String(value); },
                clear: () => { store = {}; }
            };
        })();
        $.mockClear();
        $._lastSuccess = null;

        // Ensure global webgoat object exists for customjs hook
        global.webgoat = { customjs: {} };

        // Load the updated script under test
        jest.resetModules();
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    test('login should throw when password is missing, preventing use of hard-coded password', () => {
        // Arrange
        const { login } = global;

        // Act & Assert
        expect(() => login('Jerry')).toThrow(
            'Password must be provided by the user; hard-coded passwords are not allowed.'
        );
    });

    test('login should send provided password in request body and store returned tokens', () => {
        // Arrange
        const { login } = global;
        const password = 'userSuppliedSecret';

        // Act
        login('Jerry', password);

        // Assert - verify AJAX call configuration
        expect($.mock.calls.length).toBe(1);
        const ajaxConfig = $.mock.calls[0][0];

        expect(ajaxConfig.type).toBe('POST');
        expect(ajaxConfig.url).toBe('JWT/refresh/login');
        expect(ajaxConfig.contentType).toBe('application/json');

        const body = JSON.parse(ajaxConfig.data);
        expect(body).toEqual({ user: 'Jerry', password });

        // Simulate server success response
        ajaxConfig.success({ access_token: 'ACCESS', refresh_token: 'REFRESH' });

        expect(global.localStorage.getItem('access_token')).toBe('ACCESS');
        expect(global.localStorage.getItem('refresh_token')).toBe('REFRESH');
    });

    test('newToken should send refresh token and update tokens from server response', () => {
        // Arrange
        global.localStorage.setItem('access_token', 'OLD_ACCESS');
        global.localStorage.setItem('refresh_token', 'OLD_REFRESH');

        const { newToken } = global;

        // Act
        newToken();

        // Assert - verify AJAX call configuration
        expect($.mock.calls.length).toBe(1);
        const ajaxConfig = $.mock.calls[0][0];

        expect(ajaxConfig.type).toBe('POST');
        expect(ajaxConfig.url).toBe('JWT/refresh/newToken');

        const sentBody = JSON.parse(ajaxConfig.data);
        expect(sentBody).toEqual({ refreshToken: 'OLD_REFRESH' });

        expect(ajaxConfig.headers.Authorization).toBe('Bearer OLD_ACCESS');

        // Simulate server refresh response
        ajaxConfig.success({ access_token: 'NEW_ACCESS', refresh_token: 'NEW_REFRESH' });

        expect(global.localStorage.getItem('access_token')).toBe('NEW_ACCESS');
        expect(global.localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
    });
});
