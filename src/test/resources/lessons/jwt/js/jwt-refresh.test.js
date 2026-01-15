/**
 * Delta tests for jwt-refresh.js focusing on removal of hard-coded password
 * and safer token refresh handling.
 *
 * These tests verify that:
 * - login() no longer sends the original hard-coded password literal in the request.
 * - newToken() uses tokens from the server response rather than undefined variables.
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn();
  const chainable = {
    done: function (cb) {
      // Store callback for manual invocation in tests
      ajaxMock.lastDoneCallback = cb;
      return this;
    }
  };
  ajaxMock.mockReturnValue(chainable);
  return {
    ajax: ajaxMock
  };
});

const $ = require('jquery');

// Require the script under test so that it registers its functions
require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    $.ajax.mockClear();
    $.ajax.lastDoneCallback = undefined;
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

  test('login does not send the original hard-coded password literal', () => {
    // Arrange
    const user = 'Jerry';

    // Act
    // login is defined as a global function in the script under test
    global.login(user);

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxCall = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxCall.data);

    // Ensure the password field is no longer the original hard-coded value
    expect(body.user).toBe(user);
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken stores tokens from server response rather than undefined variables', () => {
    // Arrange
    global.localStorage.setItem('access_token', 'old-access');
    global.localStorage.setItem('refresh_token', 'old-refresh');

    // Act
    global.newToken();

    // Simulate server response via the stored done callback
    const ajaxCall = $.ajax.mock.calls[0][0];
    const response = {
      access_token: 'new-access',
      refresh_token: 'new-refresh'
    };
    $.ajax.lastDoneCallback(response);

    // Assert
    expect(global.localStorage.getItem('access_token')).toBe('new-access');
    expect(global.localStorage.getItem('refresh_token')).toBe('new-refresh');
    // This verifies that the implementation uses response.* instead of undefined variables.
  });
});
