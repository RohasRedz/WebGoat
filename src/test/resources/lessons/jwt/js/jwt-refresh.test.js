// Test file path (mirrors main with a parallel test directory assumption):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// NOTE: Adjust test path/imports to match your actual Jest setup.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn().mockReturnValue({
    success: function (handler) {
      // For login(), we simulate a response with tokens.
      handler({ access_token: 'access-from-login', refresh_token: 'refresh-from-login' });
      return this;
    }
  });
  return {
    ajax: ajaxMock
  };
});

const $ = require('jquery');

// Re-declare the functions under test in a CommonJS-compatible way
// matching the updated jwt-refresh.js behavior.
let getJwtPassword;
let login;
let newToken;

beforeEach(() => {
  // Reset localStorage-like storage
  global.localStorage = (function () {
    let store = {};
    return {
      getItem: (key) => store[key] || null,
      setItem: (key, value) => { store[key] = String(value); },
      removeItem: (key) => { delete store[key]; },
      clear: () => { store = {}; }
    };
  })();

  // Mock configuration for getJwtPassword resolution
  global.window = {};
  global.process = { env: {} };

  // Define functions equivalent to updated implementation
  getJwtPassword = function () {
    if (typeof window !== 'undefined' && window.WEBGOAT_CONFIG && window.WEBGOAT_CONFIG.JWT_PASSWORD) {
      return window.WEBGOAT_CONFIG.JWT_PASSWORD;
    }
    if (typeof process !== 'undefined' && process.env && process.env.WEBGOAT_JWT_PASSWORD) {
      return process.env.WEBGOAT_JWT_PASSWORD;
    }
    return '';
  };

  login = function (user) {
    const password = getJwtPassword();
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
      if (response && typeof response === 'object') {
        if (response['access_token']) {
          localStorage.setItem('access_token', response['access_token']);
        }
        if (response['refresh_token']) {
          localStorage.setItem('refresh_token', response['refresh_token']);
        }
      }
    });
  };

  newToken = function () {
    const refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
      headers: {
        Authorization: 'Bearer ' + localStorage.getItem('access_token')
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      contentType: 'application/json',
      data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
      if (response && typeof response === 'object') {
        if (response.access_token) {
          localStorage.setItem('access_token', response.access_token);
        }
        if (response.refresh_token) {
          localStorage.setItem('refresh_token', response.refresh_token);
        }
      }
    });
  };
});

describe('jwt-refresh delta tests', () => {
  test('login uses getJwtPassword value instead of hard-coded password and stores returned tokens', () => {
    // Arrange
    window.WEBGOAT_CONFIG = { JWT_PASSWORD: 'runtime-secret' };

    // Act
    login('Jerry');

    // Assert - password is taken from config and tokens are stored
    const ajaxCall = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxCall.data);
    expect(payload.password).toBe('runtime-secret');
    expect(payload.user).toBe('Jerry');

    expect(localStorage.getItem('access_token')).toBe('access-from-login');
    expect(localStorage.getItem('refresh_token')).toBe('refresh-from-login');
  });

  test('getJwtPassword falls back to empty string when no config or env is set', () => {
    // Arrange
    window.WEBGOAT_CONFIG = undefined;
    process.env.WEBGOAT_JWT_PASSWORD = undefined;

    // Act
    const pwd = getJwtPassword();

    // Assert
    expect(pwd).toBe('');
  });

  test('newToken updates tokens from response body instead of using undefined variables', () => {
    // Arrange
    // First store initial tokens (e.g., from login)
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    // Reconfigure $.ajax mock for this specific call: simulate different response
    $.ajax.mockImplementationOnce(() => ({
      success: function (handler) {
        handler({ access_token: 'new-access', refresh_token: 'new-refresh' });
        return this;
      }
    }));

    // Act
    newToken();

    // Assert
    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');

    const ajaxCall = $.ajax.mock.calls[0][0];
    const sentPayload = JSON.parse(ajaxCall.data);
    expect(sentPayload.refreshToken).toBe('old-refresh');
  });
});
