// File path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// NOTE: This test focuses only on the changed behavior related to removal of the hard-coded
// password and the use of getWebGoatJwtPassword() within login() in jwt-refresh.js.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Simulate async success callback
      cb({ access_token: 'dummy-access', refresh_token: 'dummy-refresh' });
      return this;
    }
  }));
  const readyMock = jest.fn((cb) => cb());

  const $ = function () {};
  $.ajax = ajaxMock;
  $.fn = { ready: readyMock };
  $.ready = readyMock;

  return $;
});

const $ = require('jquery');

// Require the module under test after mocking jQuery so that it picks up the mocked version.
// In the actual project this script is loaded as a plain browser script; here we rely on
// CommonJS-style require for testing purposes.
// TODO: Adjust the relative path if bundling/runtime differs.
require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh login password source (delta tests)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure a clean config object before each test
    global.window = global.window || {};
    window.webgoat = { config: {} };
  });

  test('login should use getWebGoatJwtPassword() value from configuration', () => {
    // Arrange
    window.webgoat.config.jwtPassword = 'CONFIG_DRIVEN_PASSWORD';

    // Spy on helper to ensure it is invoked
    const getWebGoatJwtPasswordSpy = jest.spyOn(window, 'getWebGoatJwtPassword');

    // Act: trigger login via the exposed login function (attached to global scope)
    expect(typeof global.login).toBe('function');
    global.login('Jerry');

    // Assert: helper is called and jQuery.ajax is invoked with the derived password
    expect(getWebGoatJwtPasswordSpy).toHaveBeenCalled();

    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxCallArgs = $.ajax.mock.calls[0][0];

    expect(ajaxCallArgs.type).toBe('POST');
    expect(ajaxCallArgs.url).toBe('JWT/refresh/login');
    const payload = JSON.parse(ajaxCallArgs.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('CONFIG_DRIVEN_PASSWORD');
  });

  test('login should not depend on any hard-coded password literal and fall back to placeholder when config is absent', () => {
    // Arrange: no jwtPassword in config
    window.webgoat.config = {};

    const getWebGoatJwtPasswordSpy = jest.spyOn(window, 'getWebGoatJwtPassword');

    // Act
    global.login('Jerry');

    // Assert
    expect(getWebGoatJwtPasswordSpy).toHaveBeenCalled();
    const ajaxCallArgs = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxCallArgs.data);

    // The password must come from the helper's fallback value and not from any previous hard-coded secret.
    expect(payload.password).toBe('CHANGE_ME_IN_CONFIG');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
