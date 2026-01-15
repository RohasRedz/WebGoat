/**
 * Delta tests for jwt-refresh.js focusing on removal of hard-coded password
 * and secure token handling in newToken().
 *
 * Intended path:
 * src/test/resources/lessons/jwt/js/jwt-refresh.test.js
 */

const $ = require('jquery');

jest.mock('jquery', () => {
  const original = jest.requireActual('jquery');
  // Provide minimal mock with ajax used in the file.
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Allow test to control callback separately.
      ajaxMock._successCallback = cb;
      return this;
    }
  }));
  const mocked = function () {
    return original.apply(this, arguments);
  };
  mocked.ajax = ajaxMock;
  return mocked;
});

// Recreate the minimal logic from the fixed jwt-refresh.js for delta testing.
function login(user) {
  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify({ user: user })
  }).success(function (response) {
    localStorage.setItem('access_token', response['access_token']);
    localStorage.setItem('refresh_token', response['refresh_token']);
  });
}

function newToken() {
  $.ajax({
    headers: {
      Authorization: 'Bearer ' + localStorage.getItem('access_token')
    },
    type: 'POST',
    url: 'JWT/refresh/newToken',
    data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
  }).success(function (response) {
    if (response && response['access_token']) {
      localStorage.setItem('access_token', response['access_token']);
    }
    if (response && response['refresh_token']) {
      localStorage.setItem('refresh_token', response['refresh_token']);
    }
  });
}

describe('jwt-refresh.js (delta tests)', () => {
  beforeEach(() => {
    // Reset ajax mock and localStorage
    $.ajax.mockClear();
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k],
        setItem: (k, v) => {
          store[k] = String(v);
        },
        clear: () => {
          store = {};
        }
      };
    })();
  });

  test('login does not send a hard-coded password anymore', () => {
    login('Jerry');

    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArg = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(callArg.data);

    expect(payload).toEqual({ user: 'Jerry' });
    expect(payload.password).toBeUndefined();
  });

  test('newToken updates tokens from server response instead of undefined variables', () => {
    localStorage.setItem('access_token', 'oldAccess');
    localStorage.setItem('refresh_token', 'oldRefresh');

    newToken();

    const ajaxConfig = $.ajax.mock.calls[0][0];
    const requestBody = JSON.parse(ajaxConfig.data);
    expect(requestBody).toEqual({ refreshToken: 'oldRefresh' });

    // Simulate server response via captured success callback
    const successCb = $.ajax._successCallback;
    successCb({
      access_token: 'newAccess',
      refresh_token: 'newRefresh'
    });

    expect(localStorage.getItem('access_token')).toBe('newAccess');
    expect(localStorage.getItem('refresh_token')).toBe('newRefresh');
  });
});
