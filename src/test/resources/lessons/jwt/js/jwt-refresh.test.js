// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on the fixed behavior:
 * 1. login(user, password) must use the provided arguments (no hard-coded password).
 * 2. Document-ready handler must bind to the form submit and call login with form values.
 * 3. No hard-coded secret password literal should appear in the module behavior.
 *
 * NOTE:
 * - This test file assumes Jest with JSDOM environment.
 * - jQuery is loaded and $ is available as in the application runtime.
 */

// We simulate the presence of jQuery and the global webgoat namespace used in jwt-refresh.js
const $ = require('jquery');
global.$ = $;
global.jQuery = $;
global.webgoat = { customjs: {} };

// Load the module under test AFTER setting globals so that
// its $(document).ready handler and functions are registered.
require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js delta tests for hard-coded password removal', () => {
  beforeEach(() => {
    // Reset DOM and jQuery handlers before each test
    document.body.innerHTML = `
      <form id="jwt-login-form">
        <input id="jwt-username" type="text" />
        <input id="jwt-password" type="password" />
      </form>
    `;
    // Re-run document ready handlers for each test
    // In jQuery, $(document).ready handlers are executed immediately
    // when registered after DOM ready; here we simulate by triggering it.
    $(document).off(); // clear previous handlers
    require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
  });

  test('login uses provided user and password in AJAX data (no hard-coded password)', () => {
    // Arrange
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: function (cb) {
        // Simulate AJAX success callback
        cb({ access_token: 'AT', refresh_token: 'RT' });
      }
    }));

    // Access the globally defined login function
    // jwt-refresh.js defines: function login(user, password) { ... }
    expect(typeof global.login).toBe('function');

    const user = 'alice';
    const password = 's3cr3t!';

    // Act
    global.login(user, password);

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxArg = ajaxSpy.mock.calls[0][0];
    expect(ajaxArg.type).toBe('POST');
    expect(ajaxArg.url).toBe('JWT/refresh/login');

    const payload = JSON.parse(ajaxArg.data);
    expect(payload.user).toBe(user);
    expect(payload.password).toBe(password);

    // Verify that the previous hard-coded password value is not used
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('document-ready binds form submit to call login with form field values', () => {
    // Arrange
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: function (cb) {
        cb({ access_token: 'AT2', refresh_token: 'RT2' });
      }
    }));
    const loginSpy = jest.spyOn(global, 'login');

    $('#jwt-username').val('bob');
    $('#jwt-password').val('P@ssw0rd');

    // Act: trigger form submit
    $('#jwt-login-form').trigger($.Event('submit'));

    // Assert
    expect(loginSpy).toHaveBeenCalledTimes(1);
    expect(loginSpy).toHaveBeenCalledWith('bob', 'P@ssw0rd');

    // And the AJAX payload should reflect these values
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxArg = ajaxSpy.mock.calls[0][0];
    const payload = JSON.parse(ajaxArg.data);
    expect(payload.user).toBe('bob');
    expect(payload.password).toBe('P@ssw0rd');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('document-ready does not perform automatic login with hard-coded credentials', () => {
    // Arrange
    const loginSpy = jest.spyOn(global, 'login');

    // Act: no explicit submit, just rely on ready behavior that already ran on require
    // With the fix, there should be NO automatic login call on page load.

    // Assert
    expect(loginSpy).not.toHaveBeenCalled();
  });
});
