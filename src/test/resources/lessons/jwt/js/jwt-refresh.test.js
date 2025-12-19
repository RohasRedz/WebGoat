jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Simulate immediate success callback with a dummy response
      cb({ access_token: 'access', refresh_token: 'refresh' });
      return this;
    }
  }));
  return {
    ajax: ajaxMock
  };
});

const $ = require('jquery');

// Require the script under test *after* mocking jQuery
// TODO: Adjust path based on actual test runner/module resolution.
require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js – config-based password (delta test)', () => {
  beforeEach(() => {
    // Ensure clean global webgoat config and localStorage
    global.webgoat = { customjs: {}, config: {} };
    const store = {};
    global.localStorage = {
      setItem: (k, v) => { store[k] = v; },
      getItem: (k) => store[k]
    };
    jest.clearAllMocks();
  });

  test('login uses password from webgoat.config.jwtRefreshPassword when configured', () => {
    webgoat.config.jwtRefreshPassword = 'secure-config-password';

    // Call the global login function defined in jwt-refresh.js
    global.login('Jerry');

    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];

    expect(call.url).toBe('JWT/refresh/login');
    expect(call.type).toBe('POST');

    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('secure-config-password');
  });

  test('login falls back to empty password when configuration is missing (no hard-coded secret)', () => {
    // No jwtRefreshPassword set on webgoat.config

    global.login('Jerry');

    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];

    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('');
    // This ensures that we are NOT accidentally using any old hard-coded password literal.
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
