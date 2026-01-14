/**
 * Delta tests for jwt-refresh.js focusing on:
 * - removal of hard-coded password
 * - in-memory token storage and refresh behavior
 */

jest.mock('jquery', () => {
  const mockAjax = jest.fn(() => ({
    done: function (cb) {
      mockAjax._done = cb;
      return this;
    },
    fail: function (cb) {
      mockAjax._fail = cb;
      return this;
    }
  }));
  mockAjax._done = () => {};
  mockAjax._fail = () => {};
  return mockAjax;
});

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    global.webgoat = {
      customjs: {},
      getSecurePassword: jest.fn().mockReturnValue('secure-password-from-config')
    };
    // Reset jQuery ajax mocks
    $.mockClear && $.mockClear();
  });

  afterEach(() => {
    delete global.webgoat;
    jest.resetModules();
  });

  function loadModule() {
    // Simulate DOM ready
    global.document = {
      readyState: 'complete'
    };
    // Require the module under test (path relative to this test file)
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh');
  }

  test('login uses password from webgoat.getSecurePassword and not a hard-coded literal', () => {
    loadModule();

    // Call login explicitly to avoid relying on DOM ready handler details
    const loginFn = global.login || global.window?.login;
    expect(typeof loginFn).toBe('function');

    loginFn('Jerry');

    expect(webgoat.getSecurePassword).toHaveBeenCalledWith('Jerry');

    // Inspect ajax call payload
    expect($.mock.calls.length).toBeGreaterThan(0);
    const ajaxConfig = $.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('secure-password-from-config');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('tokens are stored in memory on webgoat.customjs rather than localStorage', () => {
    // Spy on localStorage if present
    const localStorage = {
      setItem: jest.fn(),
      getItem: jest.fn()
    };
    global.localStorage = localStorage;

    loadModule();

    const loginFn = global.login || global.window?.login;
    expect(typeof loginFn).toBe('function');

    loginFn('Jerry');

    // Simulate successful response from server
    const accessToken = 'access-token-123';
    const refreshToken = 'refresh-token-456';
    $.mock._done({ access_token: accessToken, refresh_token: refreshToken });

    // Verify tokens stored in memory
    expect(webgoat.customjs._accessToken).toBe(accessToken);
    expect(webgoat.customjs._refreshToken).toBe(refreshToken);

    // And not persisted via localStorage.setItem
    expect(localStorage.setItem).not.toHaveBeenCalled();
  });

  test('newToken uses in-memory tokens and updates them from server response', () => {
    loadModule();

    // Seed in-memory tokens
    webgoat.customjs._accessToken = 'old-access';
    webgoat.customjs._refreshToken = 'old-refresh';

    const newTokenFn = global.newToken || global.window?.newToken;
    expect(typeof newTokenFn).toBe('function');

    newTokenFn();

    expect($.mock.calls.length).toBeGreaterThan(0);
    const ajaxConfig = $.mock.calls[0][0];

    // Authorization header built from in-memory access token
    expect(ajaxConfig.headers.Authorization).toBe('Bearer old-access');

    // Body uses in-memory refresh token
    const body = JSON.parse(ajaxConfig.data);
    expect(body.refreshToken).toBe('old-refresh');

    // Simulate server issuing new tokens
    $.mock._done({ access_token: 'new-access', refresh_token: 'new-refresh' });

    expect(webgoat.customjs._accessToken).toBe('new-access');
    expect(webgoat.customjs._refreshToken).toBe('new-refresh');
  });
});
