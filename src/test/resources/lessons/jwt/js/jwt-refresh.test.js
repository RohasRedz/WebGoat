// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests focusing on the removal of hard-coded password and safer token handling.
// NOTE: We assume a CommonJS-compatible Jest environment where this file is loaded
// with the browser-like globals ($, localStorage, webgoat) mocked.

const $ = require('jquery');

// Provide a minimal global webgoat.customjs namespace as expected by the source.
global.webgoat = global.webgoat || {};
global.webgoat.customjs = global.webgoat.customjs || {};

// Simple in-memory mock for localStorage
class LocalStorageMock {
  constructor() {
    this.store = {};
  }
  getItem(key) {
    return this.store[key] || null;
  }
  setItem(key, value) {
    this.store[key] = String(value);
  }
  clear() {
    this.store = {};
  }
}
global.localStorage = new LocalStorageMock();

// Require the updated module after globals are in place.
require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('login() no longer sends hard-coded password literal', () => {
    // Arrange
    const ajaxSpy = jest.spyOn($, 'ajax').mockReturnValue({
      success: function (cb) {
        // simulate immediate success without calling callback (not needed here)
        return this;
      }
    });

    // Act
    // login is defined in the global scope by the imported script
    global.login('Jerry');

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    // The vulnerability fix ensures password is no longer the hard-coded secret.
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('');
  });

  test('newToken() updates tokens from response object and not from undeclared globals', () => {
    // Arrange
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((opts) => {
      return {
        success: function (cb) {
          // Simulate backend returning new tokens
          cb({
            access_token: 'new-access-token',
            refresh_token: 'new-refresh-token'
          });
          return this;
        }
      };
    });

    localStorage.setItem('access_token', 'old-access-token');
    localStorage.setItem('refresh_token', 'old-refresh-token');

    // Act
    global.newToken();

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxOptions = ajaxSpy.mock.calls[0][0];

    // Authorization header should use the existing access_token from localStorage
    expect(ajaxOptions.headers.Authorization).toBe(
      'Bearer ' + 'old-access-token'
    );

    // Body should send the stored refresh_token
    const body = JSON.parse(ajaxOptions.data);
    expect(body.refreshToken).toBe('old-refresh-token');

    // And localStorage should be updated from the response object
    expect(localStorage.getItem('access_token')).toBe('new-access-token');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh-token');
  });
});
