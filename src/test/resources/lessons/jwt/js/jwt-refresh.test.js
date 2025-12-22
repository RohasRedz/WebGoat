// File path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Jest delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and the use of a configuration abstraction instead.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Immediately invoke success callback with dummy tokens
      cb({ access_token: 'access', refresh_token: 'refresh' });
      return this;
    },
  }));
  return {
    ajax: ajaxMock,
  };
});

describe('jwt-refresh.js - delta tests for hard-coded password removal', () => {
  let $;
  let webgoat;

  beforeEach(() => {
    jest.resetModules();
    $ = require('jquery');
    // Provide global webgoat object expected by the script
    global.webgoat = { customjs: {} };
    global.localStorage = {
      data: {},
      setItem(key, value) {
        this.data[key] = value;
      },
      getItem(key) {
        return this.data[key];
      },
    };

    // Load script under test; it will attach functions to global scope and webgoat.customjs
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    webgoat = global.webgoat;
  });

  test('login should not send any hard-coded password literal in AJAX payload', () => {
    // Arrange
    // Capture the argument passed to $.ajax when login('Jerry') is called.
    const ajaxMock = $.ajax;

    // Call login directly (it is defined globally by the script)
    global.login('Jerry');

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];

    const body = JSON.parse(ajaxConfig.data);
    // Assert that password is taken from configuration and is not the old hard-coded value.
    // In the fixed code, webgoatJwtConfig.getPassword() returns null, causing login to short-circuit.
    // Thus, if password is present, this test will fail, ensuring no hard-coded password remains.
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login should abort when no password is provided by configuration', () => {
    const ajaxMock = $.ajax;
    ajaxMock.mockClear();

    // Act
    global.login('Jerry');

    // Assert: because the fixed code checks for falsy password and returns early,
    // no AJAX call must be issued in this default configuration.
    expect(ajaxMock).not.toHaveBeenCalled();
  });

  test('addBearerToken should still construct Authorization header from access_token', () => {
    localStorage.setItem('access_token', 'access-token-value');

    const headers = webgoat.customjs.addBearerToken();

    expect(headers).toEqual({
      Authorization: 'Bearer access-token-value',
    });
  });
});
