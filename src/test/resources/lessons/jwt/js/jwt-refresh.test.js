// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/**
 * Delta tests for jwt-refresh.js focusing on:
 * - removal of hard-coded password
 * - correct token refresh behavior using response values
 */

jest.mock('jquery', () => {
  const successChain = {
    success: function (cb) {
      successChain._callback = cb;
      return successChain;
    },
    _callback: null
  };

  return {
    ajax: jest.fn(() => successChain),
    __successChain: successChain
  };
});

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  let originalWebgoat;
  let originalLocalStorage;

  beforeEach(() => {
    originalWebgoat = global.webgoat;
    global.webgoat = {
      config: {
        jwtDemoPassword: 'CONFIG_PASSWORD'
      },
      customjs: {}
    };

    const store = {};
    originalLocalStorage = global.localStorage;
    global.localStorage = {
      getItem: jest.fn((key) => store[key]),
      setItem: jest.fn((key, value) => {
        store[key] = value;
      })
    };

    jest.resetModules();
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    global.localStorage = originalLocalStorage;
  });

  test('login uses configured password instead of hard-coded literal', () => {
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const successChain = $. __successChain;
    if (successChain._callback) {
      successChain._callback({ access_token: 'at', refresh_token: 'rt' });
    }

    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxArg = $.ajax.mock.calls[0][0];

    const body = JSON.parse(ajaxArg.data);
    expect(body.password).toBe('CONFIG_PASSWORD');
  });

  test('newToken updates tokens from server response and uses header Authorization', () => {
    const successChain = $. __successChain;
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    global.localStorage.getItem.mockImplementation((key) => {
      if (key === 'access_token') {
        return 'existing-access';
      }
      if (key === 'refresh_token') {
        return 'existing-refresh';
      }
      return null;
    });

    global.webgoat.customjs.addBearerToken(); // ensure function exists

    $.ajax.mockClear();
    successChain._callback = null;

    // call newToken from the module
    const jwtRefreshModule = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    if (typeof jwtRefreshModule.newToken === 'function') {
      jwtRefreshModule.newToken();
    } else if (typeof global.newToken === 'function') {
      global.newToken();
    }

    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxArg = $.ajax.mock.calls[0][0];
    expect(ajaxArg.headers.Authorization).toBe('Bearer existing-access');

    if (successChain._callback) {
      successChain._callback({
        access_token: 'new-access',
        refresh_token: 'new-refresh'
      });
    }

    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'access_token',
      'new-access'
    );
    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'refresh_token',
      'new-refresh'
    );
  });
});
