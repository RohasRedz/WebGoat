// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on removal of the hard-coded password.
 *
 * These tests verify:
 *  - login() aborts and does not send a request when no configured password is present.
 *  - login() uses the configured password from window.WEBGOAT_CONFIG.jwtPassword
 *    instead of the previous hard-coded literal.
 */

describe('jwt-refresh login delta behavior', () => {
  let originalConfig;
  let originalAjax;

  beforeEach(() => {
    originalConfig = window.WEBGOAT_CONFIG;
    originalAjax = $.ajax;
  });

  afterEach(() => {
    window.WEBGOAT_CONFIG = originalConfig;
    $.ajax = originalAjax;
  });

  test('login aborts and does not call $.ajax when password is not configured', () => {
    // Arrange
    window.WEBGOAT_CONFIG = {}; // no jwtPassword
    const ajaxSpy = jest.fn();
    $.ajax = ajaxSpy;

    // Act
    login('Jerry');

    // Assert
    expect(ajaxSpy).not.toHaveBeenCalled();
  });

  test('login uses configured password instead of a hard-coded literal', () => {
    // Arrange
    const configuredPassword = 'secureFromConfig';
    window.WEBGOAT_CONFIG = { jwtPassword: configuredPassword };

    const ajaxSpy = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
    $.ajax = ajaxSpy;

    // Act
    login('Jerry');

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArg = ajaxSpy.mock.calls[0][0];
    expect(callArg.type).toBe('POST');
    expect(callArg.url).toBe('JWT/refresh/login');

    const body = JSON.parse(callArg.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe(configuredPassword);
  });
});
