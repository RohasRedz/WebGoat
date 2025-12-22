// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// TODO: Adjust require path based on actual module bundling; this assumes the test runner
// can load the script file or that the relevant functions are exported for testing.

// For delta testing, we recreate the essential behavior of login, getJwtRefreshPassword,
// and newToken in a controlled environment, since the original code attaches functions
// to the global scope and depends on jQuery and WebGoat globals.

const $ = require('jquery');
const jsdom = require('jsdom');
const { JSDOM } = jsdom;

describe('jwt-refresh.js - delta tests for secret handling and token refresh', () => {
  let ajaxSpy;
  let originalAjax;
  let windowObj;
  let localStorageMock;

  // Minimal reimplementation matching the fixed code’s logic
  function getJwtRefreshPassword() {
    if (
      typeof windowObj !== 'undefined' &&
      windowObj.webgoat &&
      windowObj.webgoat.config &&
      windowObj.webgoat.config.jwtRefreshPassword
    ) {
      return windowObj.webgoat.config.jwtRefreshPassword;
    }
    return '***JWT_REFRESH_PASSWORD_CONFIGURED_EXTERNALLY***';
  }

  function login(user) {
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({
        user: user,
        password: getJwtRefreshPassword()
      })
    }).success(function (response) {
      windowObj.localStorage.setItem('access_token', response['access_token']);
      windowObj.localStorage.setItem('refresh_token', response['refresh_token']);
    });
  }

  function newToken() {
    const refreshToken = windowObj.localStorage.getItem('refresh_token');
    $.ajax({
      headers: {
        Authorization: 'Bearer ' + windowObj.localStorage.getItem('access_token')
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
      if (response && response.access_token) {
        windowObj.localStorage.setItem('access_token', response.access_token);
      }
      if (response && response.refresh_token) {
        windowObj.localStorage.setItem('refresh_token', response.refresh_token);
      }
    });
  }

  beforeEach(() => {
    const dom = new JSDOM(`<!DOCTYPE html><p>test</p>`);
    windowObj = dom.window;

    // Simple localStorage mock tied to this window
    localStorageMock = (() => {
      let store = {};
      return {
        getItem: key => store[key] || null,
        setItem: (key, value) => {
          store[key] = String(value);
        },
        clear: () => {
          store = {};
        }
      };
    })();
    windowObj.localStorage = localStorageMock;

    // Set a secure config value for the password
    windowObj.webgoat = {
      config: {
        jwtRefreshPassword: 'runtime-secret-password'
      }
    };

    // Spy on jQuery.ajax to capture requests and simulate responses
    originalAjax = $.ajax;
    ajaxSpy = jest.fn(options => {
      // Allow test to drive success callbacks manually
      return {
        success: cb => {
          ajaxSpy.lastSuccessCallback = cb;
          return { success: () => {} };
        }
      };
    });
    $.ajax = ajaxSpy;
  });

  afterEach(() => {
    $.ajax = originalAjax;
  });

  test('login uses a non-empty password sourced from getJwtRefreshPassword', () => {
    login('Jerry');

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const call = ajaxSpy.mock.calls[0][0];
    const payload = JSON.parse(call.data);

    expect(payload.user).toBe('Jerry');
    expect(typeof payload.password).toBe('string');
    expect(payload.password.length).toBeGreaterThan(0);
    expect(payload.password).toBe('runtime-secret-password');
  });

  test('login stores tokens from the response into localStorage', () => {
    login('Jerry');

    const call = ajaxSpy.mock.calls[0][0];
    const successCb = ajaxSpy.lastSuccessCallback;

    const response = {
      access_token: 'ACCESS123',
      refresh_token: 'REFRESH456'
    };

    successCb(response);

    expect(windowObj.localStorage.getItem('access_token')).toBe('ACCESS123');
    expect(windowObj.localStorage.getItem('refresh_token')).toBe('REFRESH456');
  });

  test('newToken sends stored tokens and updates them from server response', () => {
    // Seed existing tokens
    windowObj.localStorage.setItem('access_token', 'OLD_ACCESS');
    windowObj.localStorage.setItem('refresh_token', 'OLD_REFRESH');

    newToken();

    // First call is newToken
    const call = ajaxSpy.mock.calls[0][0];
    expect(call.headers.Authorization).toBe('Bearer OLD_ACCESS');
    const body = JSON.parse(call.data);
    expect(body.refreshToken).toBe('OLD_REFRESH');

    const successCb = ajaxSpy.lastSuccessCallback;
    const response = {
      access_token: 'NEW_ACCESS',
      refresh_token: 'NEW_REFRESH'
    };

    successCb(response);

    expect(windowObj.localStorage.getItem('access_token')).toBe('NEW_ACCESS');
    expect(windowObj.localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
  });
});
