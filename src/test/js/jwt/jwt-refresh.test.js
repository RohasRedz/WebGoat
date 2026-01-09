/**
 * Delta tests for jwt-refresh.js focusing on:
 * - No hard-coded secret value in the AJAX payload.
 * - Correct token storage behavior (access_token and refresh_token saved to localStorage).
 *
 * These tests use Jest with jsdom test environment.
 */

jest.mock('jquery', () => {
  const actualJquery = jest.requireActual('jquery');
  const mockAjax = jest.fn(() => ({
    success: function (cb) {
      // Simulate server response with tokens
      cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
      return this;
    }
  }));
  const $ = (...args) => actualJquery(...args);
  $.ajax = mockAjax;
  return $;
});

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // jsdom provides localStorage, but we reset it before each test
    localStorage.clear();
    jest.resetModules(); // ensure fresh module import to re-run IIFE
  });

  test('login on document ready stores tokens in localStorage and does not use original hard-coded password', () => {
    // Arrange
    // jsdom environment already has document; simulate ready immediately.
    document.readyState = 'complete';

    // Capture arguments passed to $.ajax
    // eslint-disable-next-line global-require
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxCallArgs = $.ajax.mock.calls[0][0];
    expect(ajaxCallArgs.type).toBe('POST');
    expect(ajaxCallArgs.url).toBe('JWT/refresh/login');
    expect(ajaxCallArgs.contentType).toBe('application/json');

    const payload = JSON.parse(ajaxCallArgs.data);
    expect(payload.user).toBe('Jerry');
    // Ensure the old hard-coded secret is not used
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
    // Ensure some non-empty password is still being sent for lesson semantics
    expect(typeof payload.password).toBe('string');
    expect(payload.password.length).toBeGreaterThan(0);

    // Tokens from simulated success callback should be stored
    expect(localStorage.getItem('access_token')).toBe('ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('REFRESH');
  });

  test('addBearerToken uses access_token from localStorage', () => {
    // Arrange
    localStorage.setItem('access_token', 'ACCESS2');
    // eslint-disable-next-line global-require
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    const headers = global.webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBe('Bearer ACCESS2');
  });
});
