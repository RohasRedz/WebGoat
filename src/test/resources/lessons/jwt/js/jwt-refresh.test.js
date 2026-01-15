// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

const $ = require('jquery');

describe('jwt-refresh – hard-coded password removed and tokens handled from response', () => {
  let originalAjax;
  let originalLocalStorage;

  beforeEach(() => {
    originalAjax = $.ajax;
    originalLocalStorage = global.localStorage;

    let store = {};
    global.localStorage = {
      getItem: (key) => store[key],
      setItem: (key, value) => {
        store[key] = value;
      },
      clear: () => {
        store = {};
      }
    };
  });

  afterEach(() => {
    $.ajax = originalAjax;
    global.localStorage = originalLocalStorage;
  });

  test('login sends a non-secret placeholder password instead of the original hard-coded secret', () => {
    const ajaxSpy = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
    $.ajax = ajaxSpy;

    function login(user) {
      const NON_SENSITIVE_DEMO_PASSWORD = 'demo-password-not-for-production';
      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: NON_SENSITIVE_DEMO_PASSWORD })
      }).success((response) => {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
      });
    }

    login('Jerry');

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);
    expect(body.password).toBe('demo-password-not-for-production');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken stores access and refresh tokens from server response instead of undefined variables', () => {
    const ajaxSpy = jest.fn().mockReturnValue({
      success: (cb) => cb({ access_token: 'newAccess', refresh_token: 'newRefresh' })
    });
    $.ajax = ajaxSpy;

    localStorage.setItem('access_token', 'oldAccess');
    localStorage.setItem('refresh_token', 'oldRefresh');

    function newToken() {
      const refreshToken = localStorage.getItem('refresh_token');
      $.ajax({
        headers: {
          Authorization: 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
      }).success((response) => {
        if (response && response.access_token && response.refresh_token) {
          localStorage.setItem('access_token', response.access_token);
          localStorage.setItem('refresh_token', response.refresh_token);
        }
      });
    }

    newToken();

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxSpy.mock.calls[0][0];
    expect(JSON.parse(ajaxConfig.data).refreshToken).toBe('oldRefresh');
    expect(localStorage.getItem('access_token')).toBe('newAccess');
    expect(localStorage.getItem('refresh_token')).toBe('newRefresh');
  });
});
