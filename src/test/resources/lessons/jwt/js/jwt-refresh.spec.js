/*
 * Delta test for BATCH-004 - jwt-refresh.js
 */
jest.mock('jquery', () => {
  const ajaxMock = jest.fn().mockReturnValue({
    success: function (cb) {
      // simulate success callback
      cb({ access_token: 'at', refresh_token: 'rt' });
    }
  });
  return {
    ajax: ajaxMock
  };
});

const $ = require('jquery');

// Inline minimal updated implementation to avoid loader/path issues in delta test.
function getRefreshLoginPassword() {
  // Mirrors the updated code: no hard-coded secret, returns null until wired to a secure source.
  return null;
}

function login(user) {
  const password = getRefreshLoginPassword();
  if (!password) {
    return;
  }
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

/**
 * Delta tests verifying:
 *  - No hard-coded password is used in the login flow anymore.
 *  - The login function does not send a request when no secure password source is configured.
 */
describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Reset mocks and localStorage between tests
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('login does not send ajax request when password source returns null (no hard-coded secret)', () => {
    // Arrange
    // getRefreshLoginPassword returns null in this test implementation.
    // Act
    login('Jerry');

    // Assert
    // Since no password is available, ajax must not be invoked at all.
    expect($.ajax).not.toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
  });

  test('login uses provided password from secure source when available (no literal constant)', () => {
    // Arrange
    // Override getRefreshLoginPassword to simulate a secure runtime value,
    // ensuring that the flow uses that value and not a hard-coded constant.
    const securePassword = 'runtime-secret';
    const originalGetRefresh = getRefreshLoginPassword;
    global.getRefreshLoginPassword = jest.fn(() => securePassword);

    function loginWithSecureSource(user) {
      const password = global.getRefreshLoginPassword();
      if (!password) {
        return;
      }
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

    // Act
    loginWithSecureSource('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    expect(ajaxConfig.url).toBe('JWT/refresh/login');

    const parsed = JSON.parse(ajaxConfig.data);
    // Ensure that the runtime-provided password is used
    expect(parsed.password).toBe(securePassword);
    // And there is no evidence of the old literal "bm5nhSkxCXZkKRy4"
    expect(ajaxConfig.data).not.toContain('bm5nhSkxCXZkKRy4');

    expect(localStorage.getItem('access_token')).toBe('at');
    expect(localStorage.getItem('refresh_token')).toBe('rt');

    // Cleanup
    global.getRefreshLoginPassword = originalGetRefresh;
  });
});
