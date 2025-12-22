// Assuming a Jest environment in a browser-like context.
// TODO: Adjust the require path based on the actual project structure/bundler.
jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      cb({ access_token: 'AT', refresh_token: 'RT' });
      return this;
    },
  }));
  return {
    ajax: ajaxMock,
  };
});

describe('jwt-refresh (delta tests for hard-coded password removal)', () => {
  let $;
  let webgoat;
  let login;
  let getJwtRefreshPassword;

  beforeEach(() => {
    // Reset module registry and localStorage
    jest.resetModules();
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => {
          store[k] = String(v);
        },
        clear: () => {
          store = {};
        },
      };
    })();

    $ = require('jquery');
    webgoat = { customjs: {} };
    global.webgoat = webgoat;

    // Load module under test
    const mod = require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // The file defines global functions; capture them
    login = global.login;
    getJwtRefreshPassword = global.getJwtRefreshPassword;
  });

  afterEach(() => {
    delete global.localStorage;
    delete global.webgoat;
    delete global.login;
    delete global.getJwtRefreshPassword;
  });

  test('login does not use the old hard-coded password literal in AJAX payload', () => {
    // Arrange
    const user = 'Jerry';

    // Act
    login(user);

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxArg = $.ajax.mock.calls[0][0];

    expect(ajaxArg.type).toBe('POST');
    expect(ajaxArg.url).toBe('JWT/refresh/login');
    expect(ajaxArg.contentType).toBe('application/json');

    const payload = JSON.parse(ajaxArg.data);
    expect(payload.user).toBe(user);

    // Critical delta behavior: password must now come from getJwtRefreshPassword(),
    // and the old secret literal must not be present.
    expect(payload.password).toBe(getJwtRefreshPassword());
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('getJwtRefreshPassword returns a non-secret placeholder (no hard-coded secret)', () => {
    const value = getJwtRefreshPassword();

    expect(typeof value).toBe('string');
    expect(value).not.toBe('bm5nhSkxCXZkKRy4');
    // Optional sanity: placeholder should be non-empty but clearly not a secret constant used before
    expect(value.length).toBeGreaterThan(0);
  });
});
