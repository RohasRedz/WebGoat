// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on the removal of the hard-coded password
 * and the introduction of getDemoPassword().
 *
 * These tests verify that:
 * - The login() function no longer sends the original hard-coded password.
 * - login() uses the value returned by getDemoPassword().
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
  return {
    ajax: ajaxMock,
    fn: {},
    ready: (fn) => fn()
  };
});

describe('jwt-refresh delta tests', () => {
  let $;

  beforeAll(() => {
    // Ensure webgoat.customjs exists for the script
    global.webgoat = { customjs: {} };
    // Load the module under test
    $ = require('jquery');
    require('../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  beforeEach(() => {
    jest.clearAllMocks();
    global.window = {};
    global.localStorage = {
      store: {},
      setItem(key, value) { this.store[key] = value; },
      getItem(key) { return this.store[key]; }
    };
  });

  test('login uses getDemoPassword() instead of hard-coded password', () => {
    // Arrange: configure webgoatConfig so getDemoPassword() returns this value
    window.webgoatConfig = { jwtDemoPassword: 'configured-secret' };

    // Act: call login from the script under test
    // login is defined in the global scope of jwt-refresh.js
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert: jquery.ajax was called with a payload that does NOT contain the old literal
    expect($.ajax).toHaveBeenCalledTimes(1);
    const args = $.ajax.mock.calls[0][0];
    const body = JSON.parse(args.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('configured-secret');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4'); // formerly hard-coded value
  });

  test('getDemoPassword falls back to empty string when no config present', () => {
    // Arrange: no webgoatConfig.jwtDemoPassword
    window.webgoatConfig = {};

    // Act
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    const args = $.ajax.mock.calls[0][0];
    const body = JSON.parse(args.data);

    expect(body.password).toBe('');
  });
});
