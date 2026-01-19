// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/**
 * Delta tests for jwt-refresh.js focusing on removal of hard-coded password:
 * - Ensures that login() uses the configured window.WEBGOAT_JWT_PASSWORD
 * - Verifies that no request is made when the password is missing/empty
 */

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  let originalAjax;
  let originalPassword;

  beforeAll(() => {
    originalAjax = $.ajax;
    originalPassword = global.window && global.window.WEBGOAT_JWT_PASSWORD;
    global.window = global.window || {};
  });

  afterAll(() => {
    $.ajax = originalAjax;
    if (originalPassword !== undefined) {
      global.window.WEBGOAT_JWT_PASSWORD = originalPassword;
    } else {
      delete global.window.WEBGOAT_JWT_PASSWORD;
    }
  });

  beforeEach(() => {
    // Reset mock for each test
    $.ajax = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
    localStorage.clear();
  });

  test('login uses configured password instead of hard-coded literal', () => {
    // Arrange
    const configuredPassword = 'secure-configured-password';
    global.window.WEBGOAT_JWT_PASSWORD = configuredPassword;
    const { login } = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];
    const body = JSON.parse(call.data);
    expect(body.password).toBe(configuredPassword);
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login does not send request when password is not configured', () => {
    delete global.window.WEBGOAT_JWT_PASSWORD;
    const { login } = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    login('Jerry');

    expect($.ajax).not.toHaveBeenCalled();
  });
});
