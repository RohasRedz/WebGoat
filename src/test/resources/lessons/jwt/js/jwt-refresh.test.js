/**
 * Delta tests for jwt-refresh.js focusing on the removal of a hard-coded password.
 *
 * Original behavior: password field in the AJAX payload was a fixed string literal.
 * Updated behavior: password is obtained at runtime via getUserPassword(), which
 * reads from the DOM (#password input) and falls back to an empty string.
 *
 * These tests validate:
 * - The AJAX call uses the dynamic password value from the DOM.
 * - The hard-coded literal is no longer required for successful invocation.
 */

const jsdom = require('jsdom');
const { JSDOM } = jsdom;

describe('jwt-refresh  delta behavior without hard-coded password', () => {
  let $;

  beforeEach(() => {
    const dom = new JSDOM(
      '<!doctype html><html><body><input id="password" type="password" value="user-secret"/></body></html>',
      { url: 'http://localhost/' }
    );
    global.window = dom.window;
    global.document = dom.window.document;

    // jQuery bound to jsdom window
    $ = require('jquery')(dom.window);
    global.$ = $;

    // Provide minimal webgoat namespace used by script
    global.webgoat = { customjs: {} };

    jest.resetModules();
  });

  test('login sends runtime password from #password instead of hard-coded literal', () => {
    // Arrange
    const ajaxSpy = jest.fn().mockReturnValue({ success: (cb) => cb({}) });
    $.ajax = ajaxSpy;

    // Load the script after setting up globals so it attaches handlers correctly
    // eslint-disable-next-line global-require
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    // Directly call login to avoid relying on document.ready timing.
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const arg = ajaxSpy.mock.calls[0][0];
    const payload = JSON.parse(arg.data);

    // The updated behavior must use the user-entered password, not a hard-coded one.
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('user-secret');
  });

  test('getUserPassword falls back to empty string when no password present', () => {
    // Arrange
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://localhost/'
    });
    global.window = dom.window;
    global.document = dom.window.document;
    $ = require('jquery')(dom.window);
    global.$ = $;
    global.webgoat = { customjs: {} };

    // eslint-disable-next-line global-require
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    // eslint-disable-next-line no-undef
    const value = getUserPassword();

    // Assert
    expect(value).toBe('');
  });
});
