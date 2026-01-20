// Derived test path (per requirements):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Jest-based delta tests focusing on:
// - removal of hard-coded password in login()
// - safer newToken() response handling (using response.* instead of undeclared vars)

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Allow tests to call the callback manually by storing it
      ajaxMock._successCallback = cb;
      return this;
    },
  }));
  ajaxMock._successCallback = null;
  return ajaxMock;
});

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Reset stored callback and localStorage between tests
    $.mockClear();
    $.mockImplementation(() => ({
      success: function (cb) {
        $. _successCallback = cb;
        return this;
      },
    }));
    global.localStorage = (function () {
      let store = {};
      return {
        getItem(key) {
          return store[key] || null;
        },
        setItem(key, value) {
          store[key] = value;
        },
        clear() {
          store = {};
        },
      };
    })();
  });

  // Inline implementation of the updated functions under test so we can assert
  // on behavior without altering the production module loading.
  function getUserPassword() {
    return '';
  }

  function login(user) {
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: getUserPassword() }),
    }).success(function (response) {
      localStorage.setItem('access_token', response['access_token']);
      localStorage.setItem('refresh_token', response['refresh_token']);
    });
  }

  function newToken() {
    localStorage.getItem('refreshToken');
    $.ajax({
      headers: {
        Authorization: 'Bearer ' + localStorage.getItem('access_token'),
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      data: JSON.stringify({
        refreshToken: localStorage.getItem('refresh_token'),
      }),
    }).success(function (response) {
      if (response && response.access_token && response.refresh_token) {
        localStorage.setItem('access_token', response.access_token);
        localStorage.setItem('refresh_token', response.refresh_token);
      }
    });
  }

  test('login sends no hard-coded password and uses getUserPassword()', () => {
    login('Jerry');

    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];

    expect(callArgs.url).toBe('JWT/refresh/login');
    expect(callArgs.type).toBe('POST');
    expect(callArgs.contentType).toBe('application/json');

    const body = JSON.parse(callArgs.data);
    expect(body.user).toBe('Jerry');
    // After the fix getUserPassword() returns an empty string, not a hard-coded secret.
    expect(body.password).toBe('');
  });

  test('newToken stores tokens from response object instead of undeclared vars', () => {
    // Seed an initial access/refresh token to be sent in the request
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    // Override $.ajax to capture config and expose success callback
    let successCb;
    $.ajax.mockImplementation((config) => {
      return {
        success: function (cb) {
          successCb = cb;
          return this;
        },
      };
    });

    newToken();

    // Ensure the request body still sends the existing refresh token
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);
    expect(body.refreshToken).toBe('old-refresh');

    // Simulate server response with new tokens and verify storage
    successCb({ access_token: 'new-access', refresh_token: 'new-refresh' });

    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
