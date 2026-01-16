// Test file path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on the removal of the hard-coded
 * password and the introduction of getDemoPassword().
 *
 * These tests verify:
 * - login() uses getDemoPassword() instead of an inline literal.
 * - getDemoPassword() prefers window.WEBGOAT_DEMO_PASSWORD when present.
 * - getDemoPassword() falls back to a non-production placeholder otherwise.
 */

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
    let originalWindow;
    let originalAjax;

    beforeEach(() => {
        originalWindow = global.window;
        global.window = {};
        originalAjax = $.ajax;
        $.ajax = jest.fn().mockReturnValue({ success: fn => fn({ access_token: 'a', refresh_token: 'r' }) });
        localStorage.clear();
    });

    afterEach(() => {
        global.window = originalWindow;
        $.ajax = originalAjax;
    });

    // Import after window and jquery have been prepared so that the module
    // can use them as expected.
    // eslint-disable-next-line global-require
    const jwtRefreshModulePath = '../../../main/resources/lessons/jwt/js/jwt-refresh.js';

    test('login uses password provided by getDemoPassword (no hard-coded literal)', () => {
        // Arrange
        // Ensure a specific demo password is set via window to avoid the fallback.
        global.window.WEBGOAT_DEMO_PASSWORD = 'from-window';

        // Load module under test
        jest.resetModules();
        // eslint-disable-next-line global-require
        require(jwtRefreshModulePath);

        // Act
        // Call login directly; it should be defined in the global scope by the module.
        // We simulate this by requiring the module and then invoking the function name.
        // eslint-disable-next-line no-undef
        login('Jerry');

        // Assert
        expect($.ajax).toHaveBeenCalledTimes(1);
        const callArgs = $.ajax.mock.calls[0][0];

        expect(callArgs.url).toBe('JWT/refresh/login');
        const body = JSON.parse(callArgs.data);

        // Confirm that the password used is derived from window.WEBGOAT_DEMO_PASSWORD
        expect(body.user).toBe('Jerry');
        expect(body.password).toBe('from-window');
    });

    test('getDemoPassword falls back to non-production placeholder when window.WEBGOAT_DEMO_PASSWORD is not set', () => {
        // Arrange
        delete global.window.WEBGOAT_DEMO_PASSWORD;
        jest.resetModules();
        // eslint-disable-next-line global-require
        const moduleExports = require(jwtRefreshModulePath);

        // The script defines getDemoPassword in the global scope, so we reference it
        // through the global object.
        // eslint-disable-next-line no-undef
        const pwd = getDemoPassword();

        // Assert
        expect(pwd).toBe('demo-password-not-for-production');
    });
});
