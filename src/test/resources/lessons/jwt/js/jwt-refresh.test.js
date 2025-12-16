const $ = require('jquery');

global.$ = $;

// Require the module under test. If the real path/module system differs, adjust require accordingly.
// TODO: Adjust path if module resolution differs in the real project.
require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js delta tests  hardcoded password removal', () => {
  let ajaxSpy;

  beforeEach(() => {
    ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: (cb) => {
        cb({ access_token: 'access', refresh_token: 'refresh' });
        return { success: jest.fn() };
      },
    }));
    global.localStorage = {
      _store: {},
      setItem(key, value) {
        this._store[key] = String(value);
      },
      getItem(key) {
        return this._store[key];
      },
      clear() {
        this._store = {};
      },
    };

    // Configure demo password as would be done by application bootstrap.
    global.window = global.window || {};
    window.WEBGOAT_JWT_DEMO_PASSWORD = 'runtime-demo-password';
  });

  afterEach(() => {
    ajaxSpy.mockRestore();
    if (global.localStorage && typeof global.localStorage.clear === 'function') {
      global.localStorage.clear();
    }
  });

  test('login uses runtime-configured password instead of any hardcoded literal', () => {
    // Arrange
    const user = 'Jerry';
    // Ensure the function is available globally as defined in the script
    expect(typeof global.login).toBe('function');

    // Act
    global.login(user);

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxArg = ajaxSpy.mock.calls[0][0];

    expect(ajaxArg.type).toBe('POST');
    expect(ajaxArg.url).toBe('JWT/refresh/login');
    expect(ajaxArg.contentType).toBe('application/json');

    const payload = JSON.parse(ajaxArg.data);
    expect(payload.user).toBe(user);
    // Password must come from window.WEBGOAT_JWT_DEMO_PASSWORD, not a hard-coded string.
    expect(payload.password).toBe('runtime-demo-password');
    // Ensure no known old hardcoded value is accidentally present
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login still stores returned tokens without logging secrets', () => {
    // Arrange
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
    expect(typeof global.login).toBe('function');

    // Act
    global.login('Jerry');

    // Assert
    expect(localStorage.getItem('access_token')).toBe('access');
    expect(localStorage.getItem('refresh_token')).toBe('refresh');
    // Ensure module did not log tokens via console.log in this flow
    const loggedArgs = consoleSpy.mock.calls.flat();
    expect(loggedArgs.join(' ')).not.toContain('access');
    expect(loggedArgs.join(' ')).not.toContain('refresh');

    consoleSpy.mockRestore();
  });
});
