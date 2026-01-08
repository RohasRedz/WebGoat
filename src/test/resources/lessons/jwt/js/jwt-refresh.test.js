// Derived from:
//   Source : src/main/resources/lessons/jwt/js/jwt-refresh.js
//   Test   : src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 * - Removal of hard-coded password in login payload.
 * - Correct usage of response tokens in newToken().
 */

describe('jwt-refresh delta tests', () => {
  let originalAjax;
  let originalLocalStorage;
  let ajaxMock;
  let storage;

  beforeEach(() => {
    ajaxMock = jest.fn(() => ({
      success: (cb) => {
        ajaxMock._successCallback = cb;
        return { success: () => {} };
      }
    }));
    global.$ = {
      ajax: ajaxMock,
      ready: (fn) => fn(),
    };

    storage = {};
    originalLocalStorage = global.localStorage;
    global.localStorage = {
      getItem: (k) => storage[k],
      setItem: (k, v) => { storage[k] = v; },
    };

    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    global.$ = undefined;
    global.localStorage = originalLocalStorage;
  });

  test('login should not include hard-coded password in request payload', () => {
    const call = ajaxMock.mock.calls[0];
    const options = call[0];

    const body = JSON.parse(options.data);
    expect(body).toHaveProperty('user');
    expect(body).not.toHaveProperty('password');
  });

  test('newToken should store tokens returned from response object', () => {
    storage['access_token'] = 'old_access';
    storage['refresh_token'] = 'old_refresh';

    ajaxMock.mockClear();
    const jwtRefreshModule = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    if (typeof jwtRefreshModule.newToken === 'function') {
      jwtRefreshModule.newToken();
    } else if (typeof global.newToken === 'function') {
      global.newToken();
    }

    const options = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(options.data);
    expect(body.refreshToken).toBe('old_refresh');

    const response = {
      access_token: 'new_access',
      refresh_token: 'new_refresh',
    };
    ajaxMock._successCallback(response);

    expect(storage['access_token']).toBe('new_access');
    expect(storage['refresh_token']).toBe('new_refresh');
  });
});
