// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/**
 * Delta tests for jwt-refresh.js ensuring the hard-coded password/secret
 * has been removed from the login payload while preserving request structure.
 */
const $ = require('jquery');

// We require the updated jwt-refresh.js, which will attach behavior
// to jQuery and window on load. In a Jest + jsdom environment, this
// is safe as long as we control the AJAX layer.
require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh (hard-coded secret removal)', () => {
  let ajaxSpy;

  beforeEach(() => {
    ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: (cb) => {
        // simulate success callback without invoking network
        cb({ access_token: 'at', refresh_token: 'rt' });
        return { success: () => {} };
      },
    }));
    // Clear any previous tokens
    localStorage.clear();
  });

  afterEach(() => {
    ajaxSpy.mockRestore();
  });

  test('login does not send a hard-coded password value', () => {
    // Arrange
    // The login function is in global scope after requiring jwt-refresh.js
    expect(typeof global.login).toBe('function');

    // Act
    global.login('Jerry');

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxSpy.mock.calls[0][0];

    expect(ajaxConfig.type).toBe('POST');
    expect(ajaxConfig.url).toBe('JWT/refresh/login');
    expect(ajaxConfig.contentType).toBe('application/json');

    const body = JSON.parse(ajaxConfig.data);
    expect(body.user).toBe('Jerry');
    // Assert that the password is no longer the hard-coded secret string
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
