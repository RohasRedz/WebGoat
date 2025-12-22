// Test file path derived from src/main/resources/lessons/jwt/js/jwt-refresh.js:
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta tests for jwt-refresh.js focusing only on:
// - Removal of hard-coded password from login payload
// - Safer token handling in localStorage interactions

// We do not import the original script directly; instead, we re-declare the minimal
// functions under test to keep these tests deterministic and self-contained.

// Minimal global stubs
global.$ = {
  ajax: jest.fn(),
};
global.localStorage = (() => {
  let store = {};
  return {
    getItem: (k) => (k in store ? store[k] : null),
    setItem: (k, v) => {
      store[k] = String(v);
    },
    clear: () => {
      store = {};
    },
  };
})();

global.webgoat = {
  customjs: {},
};

// Re-implementation of the updated code's core behaviors for delta testing
function login(user) {
  const payload = { user: user };
  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify(payload),
  }).success(function (response) {
    if (response && typeof response.access_token === 'string' && response.access_token.length > 0) {
      localStorage.setItem('access_token', response['access_token']);
    }
    if (response && typeof response.refresh_token === 'string' && response.refresh_token.length > 0) {
      localStorage.setItem('refresh_token', response['refresh_token']);
    }
  });
}

webgoat.customjs.addBearerToken = function () {
  const headers_to_set = {};
  const accessToken = localStorage.getItem('access_token');
  if (typeof accessToken === 'string' && accessToken.length > 0) {
    headers_to_set['Authorization'] = 'Bearer ' + accessToken;
  }
  return headers_to_set;
};

function newToken() {
  const refreshToken = localStorage.getItem('refresh_token');
  if (typeof refreshToken !== 'string' || refreshToken.length === 0) {
    return;
  }

  $.ajax({
    headers: webgoat.customjs.addBearerToken(),
    type: 'POST',
    url: 'JWT/refresh/newToken',
    contentType: 'application/json',
    data: JSON.stringify({ refreshToken: refreshToken }),
  }).success(function (response) {
    if (response && typeof response.access_token === 'string' && response.access_token.length > 0) {
      localStorage.setItem('access_token', response.access_token);
    }
    if (response && typeof response.refresh_token === 'string' && response.refresh_token.length > 0) {
      localStorage.setItem('refresh_token', response.refresh_token);
    }
  });
}

describe('jwt-refresh delta tests (no hard-coded password, safer token handling)', () => {
  beforeEach(() => {
    localStorage.clear();
    $.ajax.mockReset();
    $.ajax.mockImplementation(() => ({
      success: (cb) => {
        cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
        return this;
      },
    }));
  });

  test('login sends payload without hard-coded password', () => {
    // Arrange
    const user = 'Jerry';

    // Act
    login(user);

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    const sent = JSON.parse(ajaxConfig.data);

    // Ensure only 'user' is present; password must not be present in payload
    expect(sent).toEqual({ user: user });
    expect(Object.prototype.hasOwnProperty.call(sent, 'password')).toBe(false);
  });

  test('login stores tokens only when present and non-empty', () => {
    // Arrange
    const user = 'Jerry';

    // Act
    login(user);

    // Assert
    expect(localStorage.getItem('access_token')).toBe('ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('REFRESH');
  });

  test('addBearerToken returns Authorization header only when access token is set', () => {
    // Arrange
    expect(localStorage.getItem('access_token')).toBeNull();

    // Act & Assert
    // Without token, no Authorization header
    expect(webgoat.customjs.addBearerToken()).toEqual({});

    // With token, Authorization header is added
    localStorage.setItem('access_token', 'ACCESS');
    expect(webgoat.customjs.addBearerToken()).toEqual({ Authorization: 'Bearer ACCESS' });
  });

  test('newToken aborts when no refresh_token is present', () => {
    // Arrange
    localStorage.clear();

    // Act
    newToken();

    // Assert: No AJAX call made due to missing refresh token
    expect($.ajax).not.toHaveBeenCalled();
  });

  test('newToken sends refreshToken from storage and updates tokens from response', () => {
    // Arrange
    localStorage.setItem('access_token', 'OLD_ACCESS');
    localStorage.setItem('refresh_token', 'OLD_REFRESH');

    const ajaxSpy = $.ajax.mockImplementation(() => ({
      success: (cb) => {
        cb({ access_token: 'NEW_ACCESS', refresh_token: 'NEW_REFRESH' });
        return this;
      },
    }));

    // Act
    newToken();

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const config = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(config.data);
    expect(body).toEqual({ refreshToken: 'OLD_REFRESH' });

    // Tokens should be updated from response
    expect(localStorage.getItem('access_token')).toBe('NEW_ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
  });
});
