// Assumption: this test file is colocated under a Jest-enabled test root.
// Source under test:
// src/main/resources/lessons/jwt/js/jwt-refresh.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 * - Removal of hard-coded password (no literal secret present).
 * - Using configuration-derived or redacted password when sending login request.
 * - Correct token refresh handling via response object.
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Simulate a successful server response
      cb({
        access_token: 'newAccess',
        refresh_token: 'newRefresh',
      });
      return this;
    },
  }));
  return { ajax: ajaxMock };
});

describe('jwt-refresh (delta tests)', () => {
  beforeEach(() => {
    // Reset localStorage-like behavior
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => {
          store[k] = String(v);
        },
        clear: () => {
          store = {};
        },
      };
    })();
    global.webgoat = {
      config: {
        jwtDemoPassword: 'demoPasswordFromConfig',
      },
      customjs: {},
    };
    jest.resetModules();
  });

  test('login() should not use a hard-coded password literal', () => {
    // Arrange
    const { ajax } = require('jquery');
    // Require module under test
    require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    // Call login function directly (attached globally in the module).
    global.login('Jerry');

    // Assert
    expect(ajax).toHaveBeenCalledTimes(1);
    const callArgs = ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    // Ensure the password used is from configuration, not a fixed literal.
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('demoPasswordFromConfig');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken() should update tokens from server response instead of undeclared globals', () => {
    // Arrange
    const { ajax } = require('jquery');
    require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Seed existing tokens
    global.localStorage.setItem('access_token', 'oldAccess');
    global.localStorage.setItem('refresh_token', 'oldRefresh');

    // Act
    global.newToken();

    // Assert
    expect(ajax).toHaveBeenCalledTimes(1);
    expect(global.localStorage.getItem('access_token')).toBe('newAccess');
    expect(global.localStorage.getItem('refresh_token')).toBe('newRefresh');
  });

  test('addBearerToken() should not include Authorization header when token is missing', () => {
    // Arrange
    require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Ensure no access token set
    global.localStorage.clear();

    // Act
    const headers = global.webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBeUndefined();
  });
});
