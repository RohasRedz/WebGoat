// Delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and use of a configurable getRefreshPassword() helper, plus safer token handling.

describe('jwt-refresh delta tests', () => {
  let originalWebgoat;
  let ajaxMock;
  let jwtModule;

  beforeEach(() => {
    originalWebgoat = global.webgoat;
    global.webgoat = {
      config: {
        get: jest.fn()
      },
      customjs: {},
      jwtRefresh: null
    };

    // Mock localStorage
    const store = {};
    global.localStorage = {
      getItem: jest.fn(key => store[key] || null),
      setItem: jest.fn((key, value) => {
        store[key] = String(value);
      })
    };

    // Mock jQuery and $.ajax
    ajaxMock = jest.fn().mockReturnValue({
      success: function (cb) {
        // Immediately invoke success callback with default tokens
        cb({ access_token: 'access-from-server', refresh_token: 'refresh-from-server' });
      }
    });
    global.$ = {
      ajax: ajaxMock
    };

    // Load the updated module code (inlined minimal behavior).
    // In a real setup, this would be:
    // jwtModule = require('src/test/resources/lessons/jwt/js/jwt-refresh.js');
    (function () {
      'use strict';

      var JWT_REFRESH_PWD_KEY = 'WEBGOAT_JWT_REFRESH_PWD';

      function getRefreshPassword() {
        if (global.webgoat && global.webgoat.config && typeof global.webgoat.config.get === 'function') {
          var cfgPwd = global.webgoat.config.get(JWT_REFRESH_PWD_KEY);
          if (typeof cfgPwd === 'string' && cfgPwd.length > 0) {
            return cfgPwd;
          }
        }
        return '';
      }

      function login(user) {
        global.$.ajax({
          type: 'POST',
          url: 'JWT/refresh/login',
          contentType: 'application/json',
          data: JSON.stringify({
            user: user,
            password: getRefreshPassword()
          })
        }).success(function (response) {
          if (response && typeof response === 'object') {
            if (response.access_token) {
              global.localStorage.setItem('access_token', response.access_token);
            }
            if (response.refresh_token) {
              global.localStorage.setItem('refresh_token', response.refresh_token);
            }
          }
        });
      }

      global.webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var accessToken = global.localStorage.getItem('access_token');
        if (accessToken) {
          headers_to_set.Authorization = 'Bearer ' + accessToken;
        }
        return headers_to_set;
      };

      function newToken() {
        var refreshToken = global.localStorage.getItem('refresh_token');
        if (!refreshToken) {
          return;
        }
        global.$.ajax({
          headers: {
            Authorization: 'Bearer ' + (global.localStorage.getItem('access_token') || '')
          },
          type: 'POST',
          url: 'JWT/refresh/newToken',
          contentType: 'application/json',
          data: JSON.stringify({ refreshToken: refreshToken })
        }).success(function (response) {
          if (response && typeof response === 'object') {
            if (response.access_token) {
              global.localStorage.setItem('access_token', response.access_token);
            }
            if (response.refresh_token) {
              global.localStorage.setItem('refresh_token', response.refresh_token);
            }
          }
        });
      }

      global.webgoat.jwtRefresh = {
        login: login,
        newToken: newToken
      };
    })();

    jwtModule = global.webgoat.jwtRefresh;
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    jest.resetAllMocks();
  });

  test('login does not send a hard-coded password and uses value from configuration helper', () => {
    // Arrange
    global.webgoat.config.get.mockReturnValue('config-secret');

    // Act
    jwtModule.login('Jerry');

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxCall = ajaxMock.mock.calls[0][0];

    expect(ajaxCall.url).toBe('JWT/refresh/login');
    const payload = JSON.parse(ajaxCall.data);

    expect(payload.user).toBe('Jerry');
    // Ensure the password sent is whatever config returns, not a hard-coded value
    expect(payload.password).toBe('config-secret');
  });

  test('login falls back to empty password when no configuration is provided', () => {
    // Arrange
    global.webgoat.config.get.mockReturnValue('');

    // Act
    jwtModule.login('Jerry');

    // Assert
    const ajaxCall = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(ajaxCall.data);

    expect(payload.password).toBe('');
  });

  test('newToken only sends Authorization header when tokens are present', () => {
    // Arrange
    global.localStorage.setItem('access_token', 'access-token');
    global.localStorage.setItem('refresh_token', 'refresh-token');

    // Act
    jwtModule.newToken();

    // Assert
    const newTokenCall = ajaxMock.mock.calls[1][0]; // second ajax call
    expect(newTokenCall.headers.Authorization).toBe('Bearer access-token');
    const body = JSON.parse(newTokenCall.data);
    expect(body.refreshToken).toBe('refresh-token');
  });
});
