// Test file for src/main/resources/lessons/jwt/js/jwt-refresh.js
// Resolved test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on removal of hard-coded password and
 * secure token handling. We verify:
 * - The login request no longer contains the original hard-coded secret.
 * - A placeholder, non-sensitive value is used instead.
 * - Tokens from server response are stored in localStorage.
 */

describe('jwt-refresh (delta tests)', () => {
  let ajaxMock;
  let stored;

  beforeEach(() => {
    // Mock jQuery and $.ajax
    stored = {};
    global.localStorage = {
      getItem: (k) => stored[k] || null,
      setItem: (k, v) => { stored[k] = String(v); },
      removeItem: (k) => { delete stored[k]; },
      clear: () => { stored = {}; }
    };

    ajaxMock = jest.fn(() => ({
      done: (cb) => {
        cb({ access_token: 'newAccess', refresh_token: 'newRefresh' });
        return { done: () => {} };
      }
    }));

    global.$ = {
      ajax: ajaxMock,
      ready: (fn) => fn(),
      (fn) { fn(); }
    };

    global.webgoat = { customjs: {} };

    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });
  });

  afterEach(() => {
    jest.resetModules();
    delete global.$;
    delete global.webgoat;
    delete global.localStorage;
  });

  test('login sends non-sensitive placeholder instead of original hard-coded password', () => {
    const call = ajaxMock.mock.calls[0][0];

    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/login');

    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    // Ensure the old secret is not present
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
    // The placeholder should be non-empty and clearly not a real credential
    expect(typeof payload.password).toBe('string');
    expect(payload.password.length).toBeGreaterThan(0);
  });

  test('login stores tokens from server response into localStorage', () => {
    expect(localStorage.getItem('access_token')).toBe('newAccess');
    expect(localStorage.getItem('refresh_token')).toBe('newRefresh');
  });
});
