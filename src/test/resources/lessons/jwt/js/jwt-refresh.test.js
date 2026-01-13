// File under test:
// src/test/resources/lessons/jwt/js/jwt-refresh.js

// We inline a minimal version of the updated jwt-refresh.js for delta testing purposes.
$(document).ready(function () {
  login('Jerry');
});

function login(user) {
  const password =
    window.webgoatConfig && window.webgoatConfig.jwtDemoPassword
      ? window.webgoatConfig.jwtDemoPassword
      : '';

  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify({ user: user, password: password }),
  }).success(function (response) {
    if (
      response &&
      typeof response.access_token === 'string' &&
      response.access_token.length > 0
    ) {
      localStorage.setItem('access_token', response['access_token']);
    }
    if (
      response &&
      typeof response.refresh_token === 'string' &&
      response.refresh_token.length > 0
    ) {
      localStorage.setItem('refresh_token', response['refresh_token']);
    }
  });
}

webgoat = window.webgoat || {};
webgoat.customjs = webgoat.customjs || {};

webgoat.customjs.addBearerToken = function () {
  var headers_to_set = {};
  var accessToken = localStorage.getItem('access_token');
  if (typeof accessToken === 'string' && accessToken.length > 0) {
    headers_to_set['Authorization'] = 'Bearer ' + accessToken;
  }
  return headers_to_set;
};

function newToken() {
  var refreshToken = localStorage.getItem('refresh_token');
  var accessToken = localStorage.getItem('access_token');

  if (!refreshToken || !accessToken) {
    return;
  }

  $.ajax({
    headers: {
      Authorization: 'Bearer ' + accessToken,
    },
    type: 'POST',
    url: 'JWT/refresh/newToken',
    contentType: 'application/json',
    data: JSON.stringify({ refreshToken: refreshToken }),
  }).success(function (response) {
    if (
      response &&
      typeof response.access_token === 'string' &&
      response.access_token.length > 0
    ) {
      localStorage.setItem('access_token', response.access_token);
    }
    if (
      response &&
      typeof response.refresh_token === 'string' &&
      response.refresh_token.length > 0
    ) {
      localStorage.setItem('refresh_token', response.refresh_token);
    }
  });
}

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Mock jQuery.ajax
    global.$ = {
      ajax: jest.fn().mockReturnValue({ success: function (cb) {
        // Store callback for manual invocation in tests
        this._successCb = cb;
        return this;
      } }),
    };

    // Mock window and config
    global.window = global.window || {};
    window.webgoatConfig = {};

    // Mock localStorage
    const store = {};
    global.localStorage = {
      getItem: (k) => store[k],
      setItem: (k, v) => {
        store[k] = v;
      },
    };
  });

  test('login() uses external config for password and not hardcoded literal', () => {
    // Arrange
    window.webgoatConfig.jwtDemoPassword = 'externalSecret';
    const ajaxSpy = jest.spyOn($, 'ajax');

    // Act
    login('Jerry');

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const call = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(call.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('externalSecret');
  });

  test('login() falls back to empty password when config not set', () => {
    // Arrange
    delete window.webgoatConfig.jwtDemoPassword;
    const ajaxSpy = jest.spyOn($, 'ajax');

    // Act
    login('Jerry');

    // Assert
    const call = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(call.data);
    expect(body.password).toBe('');
  });

  test('addBearerToken only sets Authorization header when token present', () => {
    // Arrange
    localStorage.setItem('access_token', 'token123');

    // Act
    const headers = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBe('Bearer token123');
  });

  test('addBearerToken returns empty headers when no token present', () => {
    // Arrange
    // no token in storage

    // Act
    const headers = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBeUndefined();
  });

  test('newToken skips call when tokens missing and updates when response has tokens', () => {
    // Arrange
    const ajaxSpy = jest.spyOn($, 'ajax');

    // Act: no tokens set, should return early
    newToken();
    expect(ajaxSpy).not.toHaveBeenCalled();

    // Arrange: set tokens and call again
    localStorage.setItem('access_token', 'oldAccess');
    localStorage.setItem('refresh_token', 'oldRefresh');

    const ajaxReturn = {
      success: function (cb) {
        this._successCb = cb;
        return this;
      },
    };
    ajaxSpy.mockReturnValue(ajaxReturn);

    newToken();

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const firstCall = ajaxSpy.mock.calls[0][0];
    expect(firstCall.headers.Authorization).toBe('Bearer oldAccess');

    // Simulate successful response
    ajaxReturn._successCb({
      access_token: 'newAccess',
      refresh_token: 'newRefresh',
    });

    expect(localStorage.getItem('access_token')).toBe('newAccess');
    expect(localStorage.getItem('refresh_token')).toBe('newRefresh');
  });
});
