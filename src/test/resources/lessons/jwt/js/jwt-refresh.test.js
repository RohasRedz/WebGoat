/**
 * Delta tests for jwt-refresh.js validating:
 * - hard-coded password has been removed from the login payload
 * - token handling uses server response values instead of undefined variables
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      ajaxMock._successCallback = cb;
      return { success: () => {} };
    }
  }));
  ajaxMock._successCallback = null;
  return {
    ajax: ajaxMock,
    _ajaxMock: ajaxMock
  };
});

describe('jwt-refresh.js delta tests', () => {
  let jq;

  beforeEach(() => {
    jest.resetModules();
    jq = require('jquery');
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => { store[k] = String(v); },
        removeItem: (k) => { delete store[k]; },
        clear: () => { store = {}; }
      };
    })();
    global.webgoat = { customjs: {} };
    require('lessons/jwt/js/jwt-refresh.js'); // TODO: adjust module path to actual resolution
  });

  test('login does not send a hard-coded password in AJAX payload', () => {
    // Arrange
    const ajaxMock = jq._ajaxMock;

    // Act: trigger document.ready handler by re-requiring the module
    // login('Jerry') should have been called on ready
    expect(ajaxMock).toHaveBeenCalled();

    const lastCallArgs = ajaxMock.mock.calls[ajaxMock.mock.calls.length - 1][0];
    const payload = JSON.parse(lastCallArgs.data);

    // Assert
    expect(payload.user).toBe('Jerry');
    // After fix, password field should not contain the original hard-coded value
    expect(payload.password).toBe('');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken uses tokens from server response instead of undefined variables', () => {
    // Arrange
    const ajaxMock = jq._ajaxMock;

    // Simulate access and refresh tokens already in localStorage
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    // Call newToken from the module under test
    const mod = require('lessons/jwt/js/jwt-refresh.js'); // TODO: adjust path mapping
    if (typeof mod.newToken === 'function') {
      mod.newToken();
    } else if (typeof global.newToken === 'function') {
      global.newToken();
    }

    const lastCallArgs = ajaxMock.mock.calls[ajaxMock.mock.calls.length - 1][0];

    // Simulate server response in success callback
    const response = {
      access_token: 'server-access',
      refresh_token: 'server-refresh'
    };
    if (ajaxMock._successCallback) {
      ajaxMock._successCallback(response);
    }

    // Assert
    expect(localStorage.getItem('access_token')).toBe('server-access');
    expect(localStorage.getItem('refresh_token')).toBe('server-refresh');
  });
});
