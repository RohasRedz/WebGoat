// Derived test path (per instructions):
// src/main/resources/lessons/jwt/js/jwt-refresh.js
// -> src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Jest tests for jwt-refresh.js focusing on:
// - absence of hard-coded password in login payload
// - addBearerToken behavior with/without valid JWT
// - newToken behavior with/without refresh token and correct use of response tokens

// NOTE: We require the updated module exactly as in production path.
const fs = require('fs');
const path = require('path');

describe('jwt-refresh delta tests', () => {
  let originalAjax;

  beforeEach(() => {
    // Minimal global setup expected by the script
    global.$ = {
      ajax: jest.fn().mockReturnValue({ success: function (cb) { this._cb = cb; return this; } })
    };
    originalAjax = global.$.ajax;

    global.localStorage = (function () {
      let store = {};
      return {
        getItem: jest.fn((k) => store[k] || null),
        setItem: jest.fn((k, v) => { store[k] = String(v); }),
        clear: () => { store = {}; }
      };
    })();

    global.webgoat = { customjs: {} };

    // Load the script under test
    const scriptPath = path.resolve(__dirname, '../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const code = fs.readFileSync(scriptPath, 'utf8');
    // Execute in this context
    // eslint-disable-next-line no-eval
    eval(code);
  });

  test('login sends payload without hard-coded password literal', () => {
    // Arrange
    const callsBefore = originalAjax.mock.calls.length;

    // Act
    // Call login explicitly; function is defined in script scope.
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    const callsAfter = originalAjax.mock.calls.length;
    expect(callsAfter).toBe(callsBefore + 1);

    const lastCall = originalAjax.mock.calls[callsAfter - 1][0];
    const body = JSON.parse(lastCall.data);

    // Ensure the password is not the previous hard-coded secret
    expect(body.user).toBe('Jerry');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('addBearerToken only sets Authorization header when access_token looks like JWT', () => {
    // Arrange
    global.localStorage.getItem.mockReturnValueOnce('not-a-jwt-token');

    // eslint-disable-next-line no-undef
    const headers1 = webgoat.customjs.addBearerToken();
    expect(headers1.Authorization).toBeUndefined();

    global.localStorage.getItem.mockReturnValueOnce('header.payload.signature');

    // Act
    // eslint-disable-next-line no-undef
    const headers2 = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers2.Authorization).toBe('Bearer header.payload.signature');
  });

  test('newToken does not call ajax when no refresh_token is stored', () => {
    // Arrange
    global.localStorage.getItem.mockReturnValueOnce(null); // for refresh_token

    // Act
    // eslint-disable-next-line no-undef
    newToken();

    // Assert
    expect(originalAjax).not.toHaveBeenCalled();
  });

  test('newToken updates tokens from response when refresh_token is present', () => {
    // Arrange
    // First call to getItem: refresh_token; second: access_token
    global.localStorage.getItem
      .mockReturnValueOnce('header.payload.signature-refresh') // refresh_token
      .mockReturnValueOnce('header.payload.signature-access'); // access_token

    const ajaxMock = originalAjax;
    ajaxMock.mockReturnValue({
      success: function (cb) {
        // Simulate server response with new tokens
        cb({
          access_token: 'new-access-token',
          refresh_token: 'new-refresh-token'
        });
        return this;
      }
    });

    // Act
    // eslint-disable-next-line no-undef
    newToken();

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    expect(global.localStorage.setItem).toHaveBeenCalledWith('access_token', 'new-access-token');
    expect(global.localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'new-refresh-token');
  });
});
