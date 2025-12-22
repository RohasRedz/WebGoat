// Derived from: src/main/resources/lessons/jwt/js/jwt-refresh.js
// Test path (main -> test): src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Jest delta tests focusing only on changed behavior:
// - Hard-coded password removed and sourced from configuration.
// - newToken uses response values instead of undefined globals.
jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      // By default, invoke callback with empty object; individual tests override as needed.
      cb({});
      return { success: jest.fn() };
    }
  }));
  return {
    ajax: ajaxMock
  };
});

const $ = require('jquery');

describe('jwt-refresh.js security fixes (delta tests)', () => {
  let originalWebgoatJwtConfig;
  let originalWindow;

  beforeEach(() => {
    jest.resetModules();
    originalWebgoatJwtConfig = global.webgoatJwtConfig;
    originalWindow = global.window;

    global.window = global.window || {};
    global.webgoatJwtConfig = undefined;
    global.localStorage = {
      _store: {},
      setItem(key, value) {
        this._store[key] = String(value);
      },
      getItem(key) {
        return this._store[key] || null;
      }
    };
  });

  afterEach(() => {
    global.webgoatJwtConfig = originalWebgoatJwtConfig;
    global.window = originalWindow;
    jest.clearAllMocks();
  });

  test('login uses configurable password instead of hard-coded literal', () => {
    // Arrange
    global.webgoatJwtConfig = { jwtDemoPassword: 'CONFIGURED_SECRET' };

    // Re-require module to pick up config
    jest.isolateModules(() => {
      require('../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust relative path if needed
    });

    // Capture AJAX body
    const ajaxCall = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxCall.data);

    // Assert
    expect(body.password).toBe('CONFIGURED_SECRET');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to non-sensitive demo password when no config is provided', () => {
    // Arrange: no webgoatJwtConfig set (undefined)

    jest.isolateModules(() => {
      require('../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust relative path if needed
    });

    const ajaxCall = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxCall.data);

    // Assert: uses demo placeholder, not the old hard-coded secret
    expect(body.password).toBe('demo-password-not-for-production');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken stores tokens from response instead of undefined globals', () => {
    // Arrange
    global.localStorage.setItem('access_token', 'old-access');
    global.localStorage.setItem('refresh_token', 'old-refresh');

    // Override ajax mock for this test to capture the success callback and invoke with new tokens
    const successSpy = jest.fn();
    $.ajax.mockImplementation(() => ({
      success: (cb) => {
        successSpy(cb);
        cb({ access_token: 'new-access', refresh_token: 'new-refresh' });
        return { success: jest.fn() };
      }
    }));

    jest.isolateModules(() => {
      require('../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust relative path if needed
      // call exposed newToken
      global.window.newToken();
    });

    // Assert: tokens updated from response, not from undefined variables
    expect(global.localStorage.getItem('access_token')).toBe('new-access');
    expect(global.localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
