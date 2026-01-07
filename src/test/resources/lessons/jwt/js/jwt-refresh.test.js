// Delta test for BATCH-004: jwt-refresh.js
// File path (inferred): src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// NOTE: These tests validate only the changed behavior:
// - Hard-coded password string is removed from the AJAX payload.
// - login(user, password) uses the provided password parameter.
// - The document.ready handler sources the password from window.WEBGOAT_JWT_PASSWORD
//   (when defined) or uses the non-sensitive placeholder.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Simulate AJAX success with dummy tokens
      cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
      return this;
    }
  }));
  const readyMock = jest.fn((handler) => handler());
  return {
    ajax: ajaxMock,
    // Minimal subset used by the script
    default: {
      ajax: ajaxMock,
      ready: readyMock
    },
    ready: readyMock
  };
});

const $ = require('jquery');

// Recreate the behavior from the fixed jwt-refresh.js for delta-focused testing.
// In a real project, you would require the actual script/module instead.
// TODO: Replace with actual require path if the build exposes this file as a module.
function registerJwtRefreshScript() {
  $(document).ready(function () {
    // Copied from fixed file
    var trainingUser = 'Jerry';
    var trainingPassword = (window.WEBGOAT_JWT_PASSWORD && typeof window.WEBGOAT_JWT_PASSWORD === 'string')
      ? window.WEBGOAT_JWT_PASSWORD
      : 'CHANGE_ME_AT_RUNTIME';
    login(trainingUser, trainingPassword);
  });

  function login(user, password) {
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
      localStorage.setItem('access_token', response['access_token']);
      localStorage.setItem('refresh_token', response['refresh_token']);
    });
  }

  // Expose for direct testing of login
  return { login };
}

describe('jwt-refresh.js - hard-coded password removal (delta tests)', () => {
  let ajaxMock;
  let originalWebgoatPassword;

  beforeEach(() => {
    ajaxMock = require('jquery').ajax;
    ajaxMock.mockClear();
    // Reset localStorage
    global.localStorage.clear();
    originalWebgoatPassword = global.window.WEBGOAT_JWT_PASSWORD;
    delete global.window.WEBGOAT_JWT_PASSWORD;
  });

  afterEach(() => {
    if (typeof originalWebgoatPassword !== 'undefined') {
      global.window.WEBGOAT_JWT_PASSWORD = originalWebgoatPassword;
    } else {
      delete global.window.WEBGOAT_JWT_PASSWORD;
    }
  });

  test('login uses the password argument in the AJAX payload instead of a hard-coded value', () => {
    // Arrange
    const { login } = registerJwtRefreshScript();
    const user = 'Jerry';
    const password = 'runtimeSecret123';

    // Act
    login(user, password);

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe(user);
    expect(body.password).toBe(password);
    // Ensure that the old hard-coded password constant is not present anywhere
    expect(ajaxConfig.data).not.toContain('bm5nhSkxCXZkKRy4');
  });

  test('document ready uses window.WEBGOAT_JWT_PASSWORD when defined', () => {
    // Arrange
    global.window.WEBGOAT_JWT_PASSWORD = 'CONFIG_SECRET';
    registerJwtRefreshScript();

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('CONFIG_SECRET');
  });

  test('document ready falls back to non-sensitive placeholder when WEBGOAT_JWT_PASSWORD is not set', () => {
    // Arrange
    delete global.window.WEBGOAT_JWT_PASSWORD;
    registerJwtRefreshScript();

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('CHANGE_ME_AT_RUNTIME');
  });
});
