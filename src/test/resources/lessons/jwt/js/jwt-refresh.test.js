// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js
 * Focus: ensure hard-coded password is removed and tokens are safely handled from server response.
 */

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Reset DOM and storage mocks before each test
    global.$ = require('jquery');
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (key) => store[key] || null,
        setItem: (key, value) => {
          store[key] = String(value);
        },
        clear: () => {
          store = {};
        }
      };
    })();

    // Reset webgoat namespace
    global.webgoat = { customjs: {} };

    jest.resetModules();
  });

  test('login should not send hard-coded password in request body', () => {
    // Arrange: mock $.ajax to capture the payload
    const ajaxMock = jest.fn().mockReturnValue({ success: jest.fn() });
    global.$.ajax = ajaxMock;

    // Require the fixed module (this will define login and wire document.ready, but we call login directly)
    const scriptPath = '../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js';
    // eslint-disable-next-line global-require, import/no-dynamic-require
    const moduleExports = require(scriptPath);

    // The script defines login in global scope; call it explicitly
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const call = ajaxMock.mock.calls[0][0];

    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/login');

    const body = JSON.parse(call.data);
    expect(body.user).toBe('Jerry');
    // Critical assertion: password must not be present after the fix
    expect(body).not.toHaveProperty('password');
  });

  test('login should store tokens only when provided as strings by server', () => {
    const successHandler = jest.fn((cb) => {
      cb({ access_token: 'a-token', refresh_token: 'r-token' });
    });

    global.$.ajax = jest.fn().mockReturnValue({ success: successHandler });

    require('../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // eslint-disable-next-line no-undef
    login('Jerry');

    expect(localStorage.getItem('access_token')).toBe('a-token');
    expect(localStorage.getItem('refresh_token')).toBe('r-token');
  });

  test('newToken should not rely on global apiToken or refreshToken and should update from response', () => {
    // Arrange: set initial tokens
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    const ajaxMock = jest.fn().mockReturnValue({
      success: (cb) =>
        cb({
          access_token: 'new-access',
          refresh_token: 'new-refresh'
        })
    });
    global.$.ajax = ajaxMock;

    require('../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // eslint-disable-next-line no-undef
    newToken();

    // Assert the request uses the stored refresh token
    const payload = JSON.parse(ajaxMock.mock.calls[0][0].data);
    expect(payload.refreshToken).toBe('old-refresh');

    // And the tokens are updated from the response body, not from undeclared globals
    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
