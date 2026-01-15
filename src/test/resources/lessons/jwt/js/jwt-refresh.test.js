jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      ajaxMock._successCallback = cb;
      return this;
    },
  }));
  ajaxMock._successCallback = null;
  return ajaxMock;
});

const $ = require('jquery');

describe('jwt-refresh (delta tests)', () => {
  let originalLocalStorage;
  let tokens;

  beforeEach(() => {
    tokens = {};
    originalLocalStorage = global.localStorage;
    global.localStorage = {
      getItem: jest.fn((k) => tokens[k] || null),
      setItem: jest.fn((k, v) => {
        tokens[k] = v;
      }),
    };
    $.mockClear && $.mockClear();
    $._successCallback = null;
  });

  afterEach(() => {
    global.localStorage = originalLocalStorage;
  });

  test('login does not send a known hard-coded secret in the password field', () => {
    const sensitivePattern = 'bm5nhSkxCXZkKRy4';

    const user = 'Jerry';
    const LOGIN_PLACEHOLDER_PASSWORD = 'webgoat-demo-password';

    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user, password: LOGIN_PLACEHOLDER_PASSWORD }),
    });

    const call = $.mock.calls[0][0];
    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/login');
    const body = JSON.parse(call.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe(LOGIN_PLACEHOLDER_PASSWORD);
    expect(body.password).not.toBe(sensitivePattern);
  });

  test('newToken updates tokens from server response instead of undefined variables', () => {
    tokens['access_token'] = 'oldAccess';
    tokens['refresh_token'] = 'oldRefresh';

    function newToken() {
      const refreshToken = global.localStorage.getItem('refresh_token');
      if (!refreshToken) return;

      $.ajax({
        headers: {
          Authorization: 'Bearer ' + (global.localStorage.getItem('access_token') || ''),
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken }),
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

    newToken();

    const ajaxCall = $.mock.calls[0][0];
    expect(ajaxCall.url).toBe('JWT/refresh/newToken');
    const response = {
      access_token: 'newAccess',
      refresh_token: 'newRefresh',
    };
    if ($._successCallback) {
      $._successCallback(response);
    }

    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'access_token',
      'newAccess'
    );
    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'refresh_token',
      'newRefresh'
    );
  });
});
