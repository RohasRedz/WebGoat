// Resolved test path (from src/main/...):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 * - Removal of hard-coded password
 * - Use of externally configured WEBGOAT_JWT_LOGIN_PASSWORD
 * - Fail-closed behavior when password is not configured
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // simulate success callback
      cb({ access_token: 'access-token', refresh_token: 'refresh-token' });
      return this;
    },
  }));
  const $ = (...args) => ({ ready: (fn) => fn && fn() });
  $.ajax = ajaxMock;
  return $;
});

describe('jwt-refresh security behavior (delta tests)', () => {
  let originalWebgoat;
  let originalPasswordVar;

  beforeEach(() => {
    // Ensure global webgoat object exists
    originalWebgoat = global.webgoat;
    global.webgoat = { customjs: {} };

    // Snapshot any existing password variable
    originalPasswordVar = global.WEBGOAT_JWT_LOGIN_PASSWORD;
    delete global.WEBGOAT_JWT_LOGIN_PASSWORD;

    // Clear localStorage
    localStorage.clear();

    jest.resetModules();
  });

  afterEach(() => {
    if (originalWebgoat !== undefined) {
      global.webgoat = originalWebgoat;
    } else {
      delete global.webgoat;
    }
    if (originalPasswordVar !== undefined) {
      global.WEBGOAT_JWT_LOGIN_PASSWORD = originalPasswordVar;
    } else {
      delete global.WEBGOAT_JWT_LOGIN_PASSWORD;
    }
  });

  test('login fails closed when WEBGOAT_JWT_LOGIN_PASSWORD is not configured', () => {
    // Arrange
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const $ = require('jquery');
    const ajaxSpy = $.ajax;

    // Act: require module (this will register login function and call login("Jerry") on ready)
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Assert: ajax must not be called because password is not configured
    expect(ajaxSpy).not.toHaveBeenCalled();
    expect(consoleErrorSpy).toHaveBeenCalled();

    consoleErrorSpy.mockRestore();
  });

  test('login uses externally configured WEBGOAT_JWT_LOGIN_PASSWORD', () => {
    // Arrange
    const PASSWORD = 'secure-runtime-password';
    global.WEBGOAT_JWT_LOGIN_PASSWORD = PASSWORD;

    const $ = require('jquery');
    const ajaxSpy = $.ajax;

    // Act
    const moduleExports = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    // Call login explicitly to avoid relying on DOM ready hook
    moduleExports.login('Jerry');

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxArg = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(ajaxArg.data);
    expect(body.password).toBe(PASSWORD);
    expect(body.user).toBe('Jerry');
    expect(ajaxArg.url).toBe('JWT/refresh/login');

    // Tokens should be stored as per success callback simulation
    expect(localStorage.getItem('access_token')).toBe('access-token');
    expect(localStorage.getItem('refresh_token')).toBe('refresh-token');
  });
});
