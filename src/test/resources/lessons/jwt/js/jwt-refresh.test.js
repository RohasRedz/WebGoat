// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 * - login uses provided credentials (no hard-coded password).
 * - Tokens are stored only when successful response contains them.
 */
describe('jwt-refresh security delta tests', () => {
    let originalAjax;
    let originalGetItem;
    let originalSetItem;

    beforeEach(() => {
        // Mock jQuery.ajax
        originalAjax = $.ajax;
        $.ajax = jest.fn();

        // Mock localStorage
        originalGetItem = window.localStorage.getItem;
        originalSetItem = window.localStorage.setItem;
        window.localStorage.getItem = jest.fn();
        window.localStorage.setItem = jest.fn();
    });

    afterEach(() => {
        $.ajax = originalAjax;
        window.localStorage.getItem = originalGetItem;
        window.localStorage.setItem = originalSetItem;
        jest.clearAllMocks();
    });

    test('login should send user-supplied password, not a hard-coded one', () => {
        // Arrange
        const user = 'Jerry';
        const password = 'UserSuppliedPassword';

        // Act
        login(user, password);

        // Assert
        expect($.ajax).toHaveBeenCalledTimes(1);
        const ajaxConfig = $.ajax.mock.calls[0][0];

        const body = JSON.parse(ajaxConfig.data);
        expect(body.user).toBe(user);
        expect(body.password).toBe(password);
        // Ensure no evidence of the old hard-coded password in the payload
        expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
    });

    test('tokens are stored only when present in successful response', () => {
        // Arrange
        const user = 'Jerry';
        const password = 'UserSuppliedPassword';

        $.ajax.mockImplementation((config) => {
            const response = { access_token: 'access123', refresh_token: 'refresh456' };
            config.success(response);
        });

        // Act
        login(user, password);

        // Assert
        expect(window.localStorage.setItem).toHaveBeenCalledWith('access_token', 'access123');
        expect(window.localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'refresh456');
    });

    test('addBearerToken should only set header when access_token exists', () => {
        // Arrange
        window.localStorage.getItem.mockReturnValueOnce('access123');

        // Act
        const headers = webgoat.customjs.addBearerToken();

        // Assert
        expect(headers.Authorization).toBe('Bearer access123');
    });

    test('addBearerToken should return empty headers when no access_token is stored', () => {
        // Arrange
        window.localStorage.getItem.mockReturnValueOnce(null);

        // Act
        const headers = webgoat.customjs.addBearerToken();

        // Assert
        expect(headers.Authorization).toBeUndefined();
    });
});
