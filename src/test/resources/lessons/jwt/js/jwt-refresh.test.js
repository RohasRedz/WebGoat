// File path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta Jest tests for jwt-refresh.js focusing on removal of hard-coded password
// and safer token handling. These tests verify that:
// - login() no longer sends a hard-coded password, instead reading from configuration.
// - newToken() updates tokens based on server response instead of undeclared globals.

const $ = require('jquery');
global.$ = $;

// Require after setting globals so module initialization sees them.
require('../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: Adjust relative path according to actual project layout if needed

describe('jwt-refresh.js - delta security tests', () => {
  let ajaxSpy;

  beforeEach(() => {
    // Reset localStorage between tests
    const store = {};
    global.localStorage = {
      getItem: key => store[key],
      setItem: (key, value) => {
        store[key] = value;
      }
    };

    // Provide configuration without hard-coded secret in code
    global.window = global.window || {};
    global.window.webgoatConfig = { jwtPassword: 'configSecret' };

    ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => {
      // Return a thenable-like mock with done/fail
      return {
        done(callback) {
          callback({ access_token: 'access-from-server', refresh_token: 'refresh-from-server' });
          return this;
        },
        fail() {
          return this;
        }
      };
    });
  });

  afterEach(() => {
    if (ajaxSpy) {
      ajaxSpy.mockRestore();
    }
  });

  test('login uses password from configuration instead of hard-coded literal', () => {
    // Act: call exported login via global if available
    expect(typeof global.login).toBe('function');
    global.login('Jerry');

    // Assert: AJAX payload should contain the password from config, not the old hard-coded value.
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];
    expect(callArgs.type).toBe('POST');
    expect(callArgs.url).toBe('JWT/refresh/login');

    const payload = JSON.parse(callArgs.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('configSecret');
    // Assert the old literal is no longer present.
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken uses server response tokens instead of undeclared globals', () => {
    // Arrange: seed existing tokens
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    // Act
    expect(typeof global.newToken).toBe('function');
    global.newToken();

    // Assert: newToken should call AJAX and update tokens based on response object
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];
    expect(callArgs.url).toBe('JWT/refresh/newToken');

    expect(localStorage.getItem('access_token')).toBe('access-from-server');
    expect(localStorage.getItem('refresh_token')).toBe('refresh-from-server');
  });
});
