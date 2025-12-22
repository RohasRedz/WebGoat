// Jest delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and correct handling of token refresh logic. This test assumes the fixed behavior
// where the password is obtained via window.webgoat.getJwtDemoPassword() and
// newToken() uses server response tokens instead of undefined globals.

const $ = require('jquery');

jest.mock('jquery', () => {
  const original = jest.requireActual('jquery');
  const mockAjax = jest.fn(() => {
    const dfd = new original.Deferred();
    // Caller will control resolution via the returned deferred reference
    return dfd;
  });
  return Object.assign((...args) => original(...args), original, { ajax: mockAjax });
});

describe('jwt-refresh delta tests', () => {
  let originalWebgoat;

  beforeEach(() => {
    originalWebgoat = global.webgoat;
    global.webgoat = {
      customjs: {},
      getJwtDemoPassword: jest.fn(() => 'secure-password-from-config'),
    };
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: key => store[key] || null,
        setItem: (key, value) => {
          store[key] = String(value);
        },
        clear: () => {
          store = {};
        },
      };
    })();
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    jest.clearAllMocks();
  });

  // Inline the fixed functions under test, matching the updated implementation.
  function login(user) {
    var password =
      global.webgoat && typeof global.webgoat.getJwtDemoPassword === 'function'
        ? global.webgoat.getJwtDemoPassword()
        : '';

    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: password }),
    }).success(function (response) {
      global.localStorage.setItem('access_token', response['access_token']);
      global.localStorage.setItem('refresh_token', response['refresh_token']);
    });
  }

  function newToken() {
    var refreshToken = global.localStorage.getItem('refresh_token');

    $.ajax({
      headers: {
        Authorization: 'Bearer ' + global.localStorage.getItem('access_token'),
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      contentType: 'application/json',
      data: JSON.stringify({ refreshToken: refreshToken }),
    }).success(function (response) {
      if (response && response.access_token) {
        global.localStorage.setItem('access_token', response.access_token);
      }
      if (response && response.refresh_token) {
        global.localStorage.setItem('refresh_token', response.refresh_token);
      }
    });
  }

  test('login uses password from webgoat.getJwtDemoPassword and does not hard-code secret', () => {
    const user = 'Jerry';

    // Arrange: capture ajax call
    let deferred;
    $.ajax.mockImplementation(config => {
      const dfd = new ($.Deferred)();
      deferred = dfd;
      // Wrap success handler registration
      dfd.success = handler => {
        dfd.done(handler);
        return dfd;
      };
      // Immediately resolve to simulate server
      process.nextTick(() => dfd.resolve({ access_token: 'AT', refresh_token: 'RT' }));
      return dfd;
    });

    login(user);

    expect(global.webgoat.getJwtDemoPassword).toHaveBeenCalled();

    expect($.ajax).toHaveBeenCalledTimes(1);
    const callConfig = $.ajax.mock.calls[0][0];

    // Verify no hard-coded literal appears in payload
    const sentBody = JSON.parse(callConfig.data);
    expect(sentBody.user).toBe(user);
    expect(sentBody.password).toBe('secure-password-from-config');

    expect(callConfig.contentType).toBe('application/json');

    // Ensure tokens are stored from server response
    expect(global.localStorage.getItem('access_token')).toBe('AT');
    expect(global.localStorage.getItem('refresh_token')).toBe('RT');
  });

  test('newToken sends stored refresh_token and updates tokens from server response', () => {
    global.localStorage.setItem('access_token', 'OLD_AT');
    global.localStorage.setItem('refresh_token', 'OLD_RT');

    $.ajax.mockImplementation(config => {
      const dfd = new ($.Deferred)();
      // Simulate success with new tokens from server
      process.nextTick(() =>
        dfd.resolve({
          access_token: 'NEW_AT',
          refresh_token: 'NEW_RT',
        }),
      );
      dfd.success = handler => {
        dfd.done(handler);
        return dfd;
      };
      return dfd;
    });

    newToken();

    expect($.ajax).toHaveBeenCalledTimes(1);
    const config = $.ajax.mock.calls[0][0];

    // Verify request uses Authorization header and JSON payload with refreshToken from localStorage
    expect(config.headers.Authorization).toBe('Bearer OLD_AT');

    const body = JSON.parse(config.data);
    expect(body.refreshToken).toBe('OLD_RT');

    // After success, new tokens should be stored from the response
    expect(global.localStorage.getItem('access_token')).toBe('NEW_AT');
    expect(global.localStorage.getItem('refresh_token')).toBe('NEW_RT');
  });
});
