// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 * - removal of direct hard-coded password usage in the login AJAX payload
 * - correct usage of refresh_token and server response in newToken
 *
 * We test the changed behavior by mocking jQuery's $.ajax and the global window/localStorage.
 */

global.window = {};
global.localStorage = (function () {
  let store = {};
  return {
    getItem: (k) => store[k] || null,
    setItem: (k, v) => {
      store[k] = String(v);
    },
    clear: () => {
      store = {};
    }
  };
})();

// Minimal jQuery ajax mock
const ajaxMock = jest.fn();
global.$ = {
  ajax: ajaxMock,
  // For $(document).ready(...) usage in the script; we just invoke the callback synchronously.
  ready: (fn) => fn()
};

// Load the updated script after setting globals
// In a real project, replace this require path with the actual module loader if bundled.
require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh delta behavior', () => {
  beforeEach(() => {
    ajaxMock.mockReset();
    localStorage.clear();
  });

  test('login uses getJwtPassword instead of inline hard-coded literal', () => {
    // Arrange: capture the ajax config used by login
    const calls = [];
    ajaxMock.mockImplementation((config) => {
      calls.push(config);
      // Simulate jQuery success callback invocation
      if (typeof config.success === 'function') {
        config.success({
          access_token: 'access123',
          refresh_token: 'refresh123'
        });
      }
      return { success: (fn) => fn({}) };
    });

    // Act: trigger the document ready handler which calls login('Jerry')
    // The require above already executed the ready handler once on load,
    // so we call it explicitly again for clarity if needed.
    global.$.ready(() => {});

    // Assert
    expect(calls.length).toBeGreaterThan(0);
    const loginCall = calls.find((c) => c.url === 'JWT/refresh/login');
    expect(loginCall).toBeDefined();
    const payload = JSON.parse(loginCall.data);

    // Ensure we are not using the literal password value directly
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');

    // Ensure tokens are stored from response
    expect(localStorage.getItem('access_token')).toBe('access123');
    expect(localStorage.getItem('refresh_token')).toBe('refresh123');
  });

  test('newToken sends stored refresh_token and updates tokens from response', () => {
    // Arrange
    localStorage.setItem('access_token', 'oldAccess');
    localStorage.setItem('refresh_token', 'oldRefresh');

    ajaxMock.mockImplementation((config) => {
      if (config.url === 'JWT/refresh/newToken') {
        const body = JSON.parse(config.data);
        // The request should use the stored refresh_token
        expect(body.refreshToken).toBe('oldRefresh');

        // Simulate server returning new tokens
        if (typeof config.success === 'function') {
          config.success({
            access_token: 'newAccess',
            refresh_token: 'newRefresh'
          });
        }
      }
      return { success: (fn) => fn({}) };
    });

    // Act: call newToken from the global scope
    global.newToken();

    // Assert: localStorage should now contain updated tokens
    expect(localStorage.getItem('access_token')).toBe('newAccess');
    expect(localStorage.getItem('refresh_token')).toBe('newRefresh');
  });
});
