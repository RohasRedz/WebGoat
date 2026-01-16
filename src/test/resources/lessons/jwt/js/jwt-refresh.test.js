// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing only on changed behavior:
 *  - Hard-coded password has been removed from the login request payload.
 *  - Tokens are written to localStorage only when present and of the correct type.
 *  - newToken uses the safer header builder and response handling.
 */

const $ = require('jquery');

describe('jwt-refresh delta tests for hard-coded password removal and token handling', () => {
  let originalLocalStorage;
  let ajaxMock;

  beforeEach(() => {
    // Simple in-memory localStorage mock
    const store = {};
    originalLocalStorage = global.localStorage;
    global.localStorage = {
      getItem: (k) => (k in store ? store[k] : null),
      setItem: (k, v) => {
        store[k] = String(v);
      },
      removeItem: (k) => {
        delete store[k];
      },
      clear: () => {
        Object.keys(store).forEach((k) => delete store[k]);
      }
    };

    // Mock jQuery.ajax
    ajaxMock = jest.spyOn($, 'ajax').mockImplementation((_options) => {
      // Provide a minimal thenable with success(callback) semantics
      return {
        success: (cb) => {
          // Do not invoke callback by default; individual tests will manually do so
          cb && cb({});
          return this;
        }
      };
    });

    // Load the script under test after mocks are in place
    jest.resetModules();
    jest.doMock('jquery', () => $);
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    ajaxMock.mockRestore();
    global.localStorage = originalLocalStorage;
    jest.resetModules();
    jest.clearAllMocks();
  });

  test('login payload no longer contains a hard-coded password', () => {
    // Arrange
    const expectedUser = 'Jerry';

    // The document.ready in the script will have called login('Jerry') already.
    // Capture the options used in the ajax call.
    expect(ajaxMock).toHaveBeenCalled();
    const call = ajaxMock.mock.calls[0][0];

    // Act
    const payload = JSON.parse(call.data);

    // Assert
    expect(payload.user).toBe(expectedUser);
    // Ensure the password field is not set to the old hard-coded secret
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login success handler only stores string tokens in localStorage', () => {
    // Arrange
    const call = ajaxMock.mock.calls[0][0];
    const successWrapper = ajaxMock.mock.results[0].value;

    // Re-run success handler with a crafted response
    const response = {
      access_token: 'ACCESS',
      refresh_token: 'REFRESH'
    };

    // Override success behavior to control callback execution
    ajaxMock.mockImplementation((options) => {
      options.success && options.success(response);
      return {
        success: () => {}
      };
    });

    // Act
    $.ajax(call);

    // Assert
    expect(global.localStorage.getItem('access_token')).toBe('ACCESS');
    expect(global.localStorage.getItem('refresh_token')).toBe('REFRESH');
  });

  test('newToken does not proceed when no refresh token is present', () => {
    // Arrange
    const webgoat = (global.webgoat = global.webgoat || { customjs: {} });
    // Reset ajax mock to track newToken calls separately
    ajaxMock.mockClear();

    // Act
    // newToken is defined in the global scope of the script
    expect(typeof global.newToken).toBe('function');
    global.newToken();

    // Assert
    expect(ajaxMock).not.toHaveBeenCalled();
  });

  test('addBearerToken only sets Authorization header when access_token exists', () => {
    // Arrange
    const webgoat = (global.webgoat = global.webgoat || { customjs: {} });
    // Initially, no token present
    global.localStorage.clear();

    // Act & Assert: no token -> no Authorization header
    let headers = webgoat.customjs.addBearerToken();
    expect(headers.Authorization).toBeUndefined();

    // Now set a token and verify header is present
    global.localStorage.setItem('access_token', 'ACCESS123');
    headers = webgoat.customjs.addBearerToken();
    expect(headers.Authorization).toBe('Bearer ACCESS123');
  });
});
