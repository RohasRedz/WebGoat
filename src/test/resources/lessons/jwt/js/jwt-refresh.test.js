// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh focusing on removal of hard-coded password
 * and corrected token refresh behavior.
 *
 * - Ensures login() no longer uses the original secret literal.
 * - Ensures newToken() updates tokens from AJAX response, not undefined variables.
 */

const $ = require('jquery');

require('../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // load script under test

describe('jwt-refresh.js security fixes (delta tests)', () => {
  beforeEach(() => {
    // Reset localStorage and AJAX mocks before each test
    global.localStorage = (function () {
      let store = {};
      return {
        getItem(key) {
          return store[key] || null;
        },
        setItem(key, value) {
          store[key] = String(value);
        },
        clear() {
          store = {};
        }
      };
    })();

    jest.spyOn($, 'ajax').mockImplementation(() => {
      return {
        success: function (cb) {
          // For login, simulate server returning tokens
          cb({
            access_token: 'access123',
            refresh_token: 'refresh123'
          });
        }
      };
    });
  });

  afterEach(() => {
    $.ajax.mockRestore();
  });

  test('login does not send original hard-coded password literal', () => {
    // Arrange
    const payloads = [];
    $.ajax.mockImplementation((options) => {
      payloads.push(JSON.parse(options.data));
      return {
        success: (cb) => {
          cb({
            access_token: 'access123',
            refresh_token: 'refresh123'
          });
        }
      };
    });

    // Act
    global.login('Jerry');

    // Assert
    const sent = payloads[0];
    expect(sent.user).toBe('Jerry');
    expect(sent.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('newToken uses response tokens instead of undefined apiToken/refreshToken', () => {
    // Arrange
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    $.ajax.mockImplementation((options) => {
      // Assert request uses existing tokens
      expect(options.headers.Authorization).toBe('Bearer old-access');
      const body = JSON.parse(options.data);
      expect(body.refreshToken).toBe('old-refresh');

      return {
        success: (cb) => {
          cb({
            access_token: 'new-access',
            refresh_token: 'new-refresh'
          });
        }
      };
    });

    // Act
    global.newToken();

    // Assert
    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
