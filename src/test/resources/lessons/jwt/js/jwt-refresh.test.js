// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta tests focusing on:
// - Removal of hard-coded password
// - Sourcing password from webgoat.config.jwtLoginPassword at runtime
/**
 * These tests rely on JSDOM + Jest to intercept AJAX calls.
 */
const { JSDOM } = require('jsdom');

describe('jwt-refresh.js password sourcing (delta tests)', () => {
  let window;
  let document;
  let $;

  beforeEach(() => {
    const dom = new JSDOM(`<!doctype html><html><body></body></html>`, {
      url: 'http://localhost/'
    });
    window = dom.window;
    document = window.document;
    global.window = window;
    global.document = document;

    // Minimal jQuery-like stub for $.ajax
    $ = {
      ajax: jest.fn().mockReturnValue({
        success: function (cb) {
          cb({ access_token: 'a', refresh_token: 'r' });
          return this;
        }
      })
    };
    global.$ = $;

    // Provide global webgoat object with configurable password
    global.webgoat = {
      config: {
        jwtLoginPassword: 'runtime-secret'
      },
      customjs: {}
    };

    // localStorage stub
    const storage = {};
    global.localStorage = {
      setItem: (k, v) => { storage[k] = v; },
      getItem: (k) => storage[k]
    };

    // Load module under test
    // eslint-disable-next-line global-require, import/no-unresolved
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
    delete global.webgoat;
    delete global.$;
    delete global.window;
    delete global.document;
    delete global.localStorage;
  });

  test('login should use password from webgoat.config.jwtLoginPassword instead of hard-coded literal', () => {
    // Arrange
    const user = 'Jerry';

    // Act
    // login is defined globally in the script
    // eslint-disable-next-line no-undef
    login(user);

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(call.data);

    expect(payload.user).toBe(user);
    expect(payload.password).toBe('runtime-secret');
  });

  test('login should not fall back to the old hard-coded password when config is missing', () => {
    // Arrange
    delete global.webgoat.config;
    const user = 'Jerry';

    // Act
    // eslint-disable-next-line no-undef
    login(user);

    // Assert
    const call = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(call.data);

    expect(payload.user).toBe(user);
    expect(payload.password).toBe('');
  });
});
