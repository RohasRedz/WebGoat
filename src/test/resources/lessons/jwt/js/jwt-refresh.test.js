// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta tests for jwt-refresh.js focusing on:
// - Removal of hard-coded password
// - Correct handling of tokens from server response during login and refresh flows

// NOTE: These tests assume the updated jwt-refresh.js module is loaded in the test environment.

describe('jwt-refresh delta tests', () => {
    beforeEach(() => {
        // Reset localStorage between tests
        localStorage.clear();
        // Ensure webgoat.customjs exists
        global.webgoat = global.webgoat || {};
        webgoat.customjs = webgoat.customjs || {};
    });

    test('login should not send hard-coded password literal', () => {
        // Arrange
        const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((options) => {
            // Simulate server response
            const response = {
                access_token: 'access-token-from-server',
                refresh_token: 'refresh-token-from-server'
            };
            options.success && options.success(response);
            return { success: (cb) => cb(response) };
        });

        // Act
        login('Jerry');

        // Assert
        expect(ajaxSpy).toHaveBeenCalledTimes(1);
        const callArgs = ajaxSpy.mock.calls[0][0];

        expect(callArgs.url).toBe('JWT/refresh/login');

        const sent = JSON.parse(callArgs.data);
        // New behavior: password should no longer be the original hard-coded literal
        expect(sent.user).toBe('Jerry');
        expect(sent.password).not.toBe('bm5nhSkxCXZkKRy4');

        // Ensure tokens from response are stored
        expect(localStorage.getItem('access_token')).toBe('access-token-from-server');
        expect(localStorage.getItem('refresh_token')).toBe('refresh-token-from-server');

        ajaxSpy.mockRestore();
    });

    test('newToken should send Authorization header using stored access token and update tokens from response', () => {
        // Arrange
        localStorage.setItem('access_token', 'existing-access-token');
        localStorage.setItem('refresh_token', 'existing-refresh-token');

        const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((options) => {
            // Verify headers are set via addBearerToken
            expect(options.headers.Authorization).toBe('Bearer existing-access-token');

            const response = {
                access_token: 'new-access-token',
                refresh_token: 'new-refresh-token'
            };
            options.success && options.success(response);
            return { success: (cb) => cb(response) };
        });

        // Act
        newToken();

        // Assert
        expect(ajaxSpy).toHaveBeenCalledTimes(1);
        const callArgs = ajaxSpy.mock.calls[0][0];
        expect(callArgs.url).toBe('JWT/refresh/newToken');

        const sent = JSON.parse(callArgs.data);
        expect(sent.refreshToken).toBe('existing-refresh-token');

        expect(localStorage.getItem('access_token')).toBe('new-access-token');
        expect(localStorage.getItem('refresh_token')).toBe('new-refresh-token');

        ajaxSpy.mockRestore();
    });

    test('newToken should no-op when refresh token is missing', () => {
        // Arrange
        localStorage.removeItem('refresh_token');
        const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => {
            return { success: (cb) => cb({}) };
        });

        // Act
        newToken();

        // Assert
        expect(ajaxSpy).not.toHaveBeenCalled();

        ajaxSpy.mockRestore();
    });
});
