// Delta tests for jwt-refresh.js focusing on hard-coded password removal:
// - Verifies that the password is taken from window.WEBGOAT_JWT_PASSWORD when set.
// - Verifies that the password falls back to an empty string when not configured.

const $ = require('jquery');

// Require the script to attach login and other functions to global scope.
// TODO: Adjust path if bundling/transpilation is used in the actual project.
require('../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh login password handling (delta tests)', () => {
  beforeEach(() => {
    // Reset globals before each test
    global.window = {};
    global.localStorage = {
      _data: {},
      setItem(key, value) {
        this._data[key] = value;
      },
      getItem(key) {
        return this._data[key];
      }
    };

    jest.spyOn($, 'ajax').mockImplementation((options) => {
      // Simulate immediate success callback
      if (typeof options.success === 'function') {
        options.success({ access_token: 'acc', refresh_token: 'ref' });
      }
      return { success: (cb) => cb({}) };
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('login uses window.WEBGOAT_JWT_PASSWORD when configured', () => {
    // Arrange
    global.window.WEBGOAT_JWT_PASSWORD = 'runtime-secret';

    // Act
    // login is defined in jwt-refresh.js and attached to global scope
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtime-secret');
  });

  test('login falls back to empty password when window.WEBGOAT_JWT_PASSWORD is undefined', () => {
    // Arrange
    delete global.window.WEBGOAT_JWT_PASSWORD;

    // Act
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('');
  });
});
