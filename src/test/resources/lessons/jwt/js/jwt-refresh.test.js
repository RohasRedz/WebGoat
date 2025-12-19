// Jest delta tests for jwt-refresh.js focusing on:
// - Removal of hard-coded password.
// - getJwtPassword() preferring runtime configuration.
// - login() using getJwtPassword() value.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      cb({ access_token: 'access', refresh_token: 'refresh' });
      return { success: jest.fn() };
    },
  }));
  const readyMock = jest.fn((cb) => cb());
  const $ = jest.fn(() => ({ ready: readyMock }));
  $.ajax = ajaxMock;
  $.ready = readyMock;
  return $;
});

describe('jwt-refresh delta tests', () => {
  let originalWebgoatConfig;
  let originalJwtPassword;

  beforeEach(() => {
    jest.resetModules();
    originalWebgoatConfig = global.window?.webgoatConfig;
    originalJwtPassword = global.window?.JWT_PASSWORD;
    global.window = global.window || {};
    delete global.window.webgoatConfig;
    delete global.window.JWT_PASSWORD;
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    if (originalWebgoatConfig !== undefined) {
      global.window.webgoatConfig = originalWebgoatConfig;
    } else {
      delete global.window.webgoatConfig;
    }
    if (originalJwtPassword !== undefined) {
      global.window.JWT_PASSWORD = originalJwtPassword;
    } else {
      delete global.window.JWT_PASSWORD;
    }
    jest.resetModules();
  });

  test('getJwtPassword uses window.webgoatConfig.jwtPassword when available', () => {
    const passwordValue = 'runtimeSecret';
    global.window.webgoatConfig = { jwtPassword: passwordValue };

    const { getJwtPassword } = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    expect(getJwtPassword()).toBe(passwordValue);
  });

  test('getJwtPassword falls back to window.JWT_PASSWORD when config is absent', () => {
    const passwordValue = 'envSecret';
    delete global.window.webgoatConfig;
    global.window.JWT_PASSWORD = passwordValue;

    const { getJwtPassword } = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    expect(getJwtPassword()).toBe(passwordValue);
  });

  test('getJwtPassword uses placeholder when no runtime sources are available', () => {
    delete global.window.webgoatConfig;
    delete global.window.JWT_PASSWORD;

    const { getJwtPassword } = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    expect(getJwtPassword()).toBe('CHANGE_ME_SECURELY_AT_RUNTIME');
  });

  test('login uses password returned by getJwtPassword instead of hard-coded literal', () => {
    const $ = require('jquery');
    const ajaxMock = $.ajax;

    global.window.webgoatConfig = { jwtPassword: 'runtimeSecret' };
    const { login } = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    ajaxMock.mockClear();
    login('Jerry');

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const callArgs = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtimeSecret');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
