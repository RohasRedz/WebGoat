// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta tests for updated jwt-refresh.js behavior focusing on:
// - Removal of hard-coded password
// - Conditional inclusion of password from DOM configuration
// - Correct handling of access/refresh tokens on login and refresh.

require('jest-localstorage-mock');

global.$ = require('jquery');

describe('jwt-refresh delta tests - secure credential handling and token flow', () => {
  let originalQuerySelector;

  beforeEach(() => {
    jest.resetAllMocks();
    localStorage.clear();

    // Mock document.querySelector used by getLoginPassword
    originalQuerySelector = document.querySelector;
    document.querySelector = jest.fn();

    // Stub $.ajax to avoid real network calls
    jest.spyOn($, 'ajax').mockImplementation((options) => {
      // Simulate jQuery's jqXHR with success callback
      const jqXHR = {
        success: function (cb) {
          if (typeof cb === 'function') {
            cb({
              access_token: 'access_from_server',
              refresh_token: 'refresh_from_server'
            });
          }
          return jqXHR;
        }
      };
      return jqXHR;
    });

    // Re-require module under test after mocks are set
    jest.isolateModules(() => {
      // Clear any cached version so that code runs again with current mocks
      jest.resetModules();
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });
  });

  afterEach(() => {
    document.querySelector = originalQuerySelector;
  });

  test('does not send a hard-coded password literal in login payload', () => {
    const ajaxCall = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxCall.data);

    expect(payload.user).toBe('Jerry');
    // Ensure password is either absent or not the original hard-coded value
    if (Object.prototype.hasOwnProperty.call(payload, 'password')) {
      expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
    }
  });

  test('includes password from DOM when data-jwt-refresh-password attribute is present', () => {
    // Arrange DOM to provide a configured password/token
    const mockElement = {
      getAttribute: jest.fn().mockReturnValue('configured-secret')
    };
    document.querySelector.mockReturnValue(mockElement);

    // Re-load module so that login() uses new querySelector behavior
    jest.isolateModules(() => {
      jest.resetModules();
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    const ajaxCall = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxCall.data);

    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('configured-secret');
  });

  test('stores access and refresh tokens from login response in localStorage', () => {
    // First $.ajax call is from login() triggered on document.ready
    const ajaxCall = $.ajax.mock.calls[0][0];
    const jqXHR = $.ajax.mock.results[0].value;

    // Simulate success callback to mimic server response
    jqXHR.success(ajaxCall.success);

    expect(localStorage.getItem('access_token')).toBe('access_from_server');
    expect(localStorage.getItem('refresh_token')).toBe('refresh_from_server');
  });

  test('newToken() updates tokens from server response without using undeclared variables', () => {
    // Prepare access and refresh tokens before calling newToken
    localStorage.setItem('access_token', 'old_access');
    localStorage.setItem('refresh_token', 'old_refresh');

    // First call is login, second call will be newToken when invoked
    const newTokenIndex = $.ajax.mock.calls.length;

    // Call newToken from module under test
    const module = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    if (typeof module.newToken === 'function') {
      module.newToken();
    }

    const newTokenCall = $.ajax.mock.calls[newTokenIndex][0];
    const jqXHR = $.ajax.mock.results[newTokenIndex].value;
    jqXHR.success(function (response) {
      return response;
    });

    // Because our stub passes {access_token, refresh_token}, tokens should be updated
    expect(localStorage.getItem('access_token')).toBe('access_from_server');
    expect(localStorage.getItem('refresh_token')).toBe('refresh_from_server');
    expect(newTokenCall.headers.Authorization).toBe('Bearer old_access');
  });
});
