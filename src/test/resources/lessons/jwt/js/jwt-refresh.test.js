/**
 * Delta tests for jwt-refresh.js focusing ONLY on the changed behavior that
 * removed the hard-coded password and corrected token handling logic.
 *
 * The fix:
 *  - Replaced the hard-coded password string with a non-secret demo password
 *    returned by getLessonDemoPassword().
 *  - Wrapped token storage into storeTokens() and fixed newToken() to use
 *    the server's response instead of undeclared variables.
 *
 * These Jest tests assert:
 *  - login() sends a non-empty, non-hardcoded password field.
 *  - Tokens from the login response are stored via storeTokens().
 *  - newToken() sends the stored refresh_token and updates tokens from the response.
 */

// TODO: Adjust the module path if the bundler/loader path differs.
const $ = require('jquery');

describe('jwt-refresh (delta tests for hard-coded password and token handling)', () => {
  let originalAjax;
  let originalLocalStorage;

  beforeAll(() => {
    originalAjax = $.ajax;

    // Simple in-memory localStorage mock
    originalLocalStorage = global.localStorage;
    const store = {};
    global.localStorage = {
      getItem: (k) => (k in store ? store[k] : null),
      setItem: (k, v) => {
        store[k] = String(v);
      },
      removeItem: (k) => {
        delete store[k];
      },
      clear: () => {
        Object.keys(store).forEach((k) => delete store[k]);
      }
    };

    // Require the module under test after mocks are in place
    // eslint-disable-next-line global-require
    require('../js/jwt-refresh');
  });

  afterAll(() => {
    $.ajax = originalAjax;
    global.localStorage = originalLocalStorage;
  });

  beforeEach(() => {
    // Reset ajax mock each test
    $.ajax = jest.fn();
    global.localStorage.clear();
  });

  test('login() should not send the original hard-coded password value', () => {
    // Arrange
    const captured = {};
    $.ajax.mockImplementation((options) => {
      captured.options = options;
      return {
        success(fn) {
          // Simulate server returning tokens
          fn({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
        }
      };
    });

    // Act
    // login is defined in the module's IIFE; invoke through the global wrapper if exposed,
    // or re-trigger the ready handler by calling login explicitly if bound.
    // For this delta test, we call login via the global function name if available.
    if (typeof global.login === 'function') {
      global.login('Jerry');
    } else {
      // TODO: If login is not globally exposed, this test will need adaptation
      // based on the actual export pattern. For now, we fail loudly.
      throw new Error('login function is not globally accessible for testing');
    }

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const body = JSON.parse(captured.options.data);

    // Ensure password is present but is not the original hardcoded secret value
    expect(body.password).toBeDefined();
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');

    // And that demo tokens are stored
    expect(global.localStorage.getItem('access_token')).toBe('ACCESS');
    expect(global.localStorage.getItem('refresh_token')).toBe('REFRESH');
  });

  test('newToken() should send stored refresh_token and update tokens from server response', () => {
    // Arrange
    global.localStorage.setItem('access_token', 'OLD_ACCESS');
    global.localStorage.setItem('refresh_token', 'OLD_REFRESH');

    const captured = {};
    $.ajax.mockImplementation((options) => {
      captured.options = options;
      return {
        success(fn) {
          fn({ access_token: 'NEW_ACCESS', refresh_token: 'NEW_REFRESH' });
        }
      };
    });

    // Act
    if (typeof global.jwtRefreshNewToken === 'function') {
      global.jwtRefreshNewToken();
    } else if (typeof global.newToken === 'function') {
      global.newToken();
    } else {
      // TODO: If newToken is not globally exposed, adapt to actual export pattern.
      throw new Error('newToken function is not globally accessible for testing');
    }

    // Assert: refresh_token from storage must be sent in the request body
    expect($.ajax).toHaveBeenCalledTimes(1);
    const body = JSON.parse(captured.options.data);
    expect(body.refreshToken).toBe('OLD_REFRESH');

    // Tokens in localStorage should be updated from server response
    expect(global.localStorage.getItem('access_token')).toBe('NEW_ACCESS');
    expect(global.localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
  });
});
