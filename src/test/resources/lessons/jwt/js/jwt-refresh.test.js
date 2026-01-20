// Tests for updated jwt-refresh.js focusing on secret handling and login behavior.

jest.mock('jquery', () => ({
  ajax: jest.fn(() => ({
    success: function (cb) {
      // Default: simulate no response body
      cb({});
      return this;
    },
  })),
}));

const $ = require('jquery');

describe('jwt-refresh.js delta tests', () => {
  let originalWebgoat;

  beforeEach(() => {
    originalWebgoat = global.webgoat;
    global.webgoat = { customjs: {} };
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => {
          store[k] = String(v);
        },
        removeItem: (k) => {
          delete store[k];
        },
        clear: () => {
          store = {};
        },
      };
    })();

    jest.resetModules();
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    delete global.localStorage;
    jest.clearAllMocks();
  });

  test('login aborts safely when password configuration is missing', () => {
    // No getJwtRefreshPassword hook configured
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const login = global.login;
    const consoleErrorSpy = jest
      .spyOn(console, 'error')
      .mockImplementation(() => {});

    login('Jerry');

    expect($.ajax).not.toHaveBeenCalled();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });

  test('login uses configured password and does not contain hard-coded literal', () => {
    global.webgoat.customjs.getJwtRefreshPassword = jest
      .fn()
      .mockReturnValue('configured-secret');

    const fs = require('fs');
    const path = require('path');
    const filePath = path.resolve(
      __dirname,
      '../../../../../main/resources/lessons/jwt/js/jwt-refresh.js'
    );
    const source = fs.readFileSync(filePath, 'utf8');

    expect(source).not.toMatch(/bm5nhSkxCXZkKRy4/);

    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const login = global.login;

    login('Jerry');

    expect(global.webgoat.customjs.getJwtRefreshPassword).toHaveBeenCalled();
    expect($.ajax).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: 'Jerry', password: 'configured-secret' }),
      })
    );
  });

  test('newToken updates tokens when response contains new values', () => {
    // Configure ajax mock to provide new tokens
    $.ajax.mockImplementation(() => ({
      success: function (cb) {
        cb({ access_token: 'new-access', refresh_token: 'new-refresh' });
        return this;
      },
    }));

    global.localStorage.setItem('access_token', 'old-access');
    global.localStorage.setItem('refresh_token', 'old-refresh');

    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const newToken = global.newToken;

    newToken();

    expect(global.localStorage.getItem('access_token')).toBe('new-access');
    expect(global.localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
