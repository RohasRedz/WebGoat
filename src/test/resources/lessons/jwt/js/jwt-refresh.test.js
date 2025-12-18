// Assuming a Node-compatible test environment; adjust path if project structure differs.
// Test file location (derived by replacing "main" with "test"):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// TODO: Adjust the relative path to jwt-refresh.js according to your actual test runner setup.
jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Immediately invoke success callback with a fake response
      cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
      return this;
    }
  }));
  return {
    ajax: ajaxMock,
    // Provide minimal jQuery interface needed by the module
    ready: (fn) => fn()
  };
});

describe('jwt-refresh.js delta tests', () => {
  let originalWebgoat;
  let originalWindowPassword;

  beforeEach(() => {
    jest.resetModules();
    global.localStorage = (() => {
      let store = {};
      return {
        getItem: (k) => store[k],
        setItem: (k, v) => { store[k] = String(v); },
        removeItem: (k) => { delete store[k]; },
        clear: () => { store = {}; }
      };
    })();

    // Preserve any existing global objects to restore later
    originalWebgoat = global.webgoat;
    originalWindowPassword = global.WEBGOAT_JWT_DEMO_PASSWORD;
    global.webgoat = { customjs: {} };
  });

  afterEach(() => {
    global.localStorage.clear();
    global.webgoat = originalWebgoat;
    global.WEBGOAT_JWT_DEMO_PASSWORD = originalWindowPassword;
  });

  test('login uses getJwtDemoPassword and honors runtime-configured password', () => {
    // Arrange
    const configuredPassword = 'RUNTIME_CONFIG_PASSWORD';
    global.WEBGOAT_JWT_DEMO_PASSWORD = configuredPassword;

    // Require the module under test AFTER globals are set
    // TODO: Update this path mapping to match your bundler/runtime
    const jwtModule = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const $ = require('jquery');
    const ajaxMock = $.ajax;

    // Act
    // Call login explicitly with a known user
    jwtModule.login('Jerry');

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const callConfig = ajaxMock.mock.calls[0][0];

    expect(callConfig.type).toBe('POST');
    expect(callConfig.url).toBe('JWT/refresh/login');

    const sentBody = JSON.parse(callConfig.data);
    expect(sentBody.user).toBe('Jerry');
    // Critical delta assertion: password is sourced from runtime config, not a hard-coded literal.
    expect(sentBody.password).toBe(configuredPassword);
  });

  test('login falls back to non-sensitive placeholder when no runtime config is provided', () => {
    // Arrange
    delete global.WEBGOAT_JWT_DEMO_PASSWORD;

    // Require fresh module to ensure it reads current global state
    jest.resetModules();
    const jwtModule = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const $ = require('jquery');
    const ajaxMock = $.ajax;

    // Act
    jwtModule.login('Jerry');

    // Assert
    const callConfig = ajaxMock.mock.calls[0][0];
    const sentBody = JSON.parse(callConfig.data);

    expect(sentBody.user).toBe('Jerry');
    // Delta: ensure the original hard-coded secret value is not present anymore.
    expect(sentBody.password).not.toBe('bm5nhSkxCXZkKRy4');
    // And that the placeholder from getJwtDemoPassword is used instead.
    expect(sentBody.password).toBe('CHANGE_ME_JWT_DEMO_PASSWORD');
  });
});
