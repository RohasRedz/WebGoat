// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta unit tests for jwt-refresh.js focusing on:
 * 1) Ensuring login is called with a non-empty password when #password has a value.
 * 2) Ensuring no hard-coded password literals remain in the implementation.
 *
 * Assumptions:
 * - This test is executed in a Jest environment with JSDOM enabled.
 * - The production file src/main/resources/lessons/jwt/js/jwt-refresh.js defines:
 *   - $(document).ready(...) which reads #username and #password and calls login(user, password)
 *     only when password is non-empty.
 *   - A function login(user, password) that issues the AJAX request.
 */

const fs = require('fs');
const path = require('path');

// We will spy on $.ajax and simulate a DOM with #username and #password
describe('jwt-refresh.js delta tests', () => {
  let $;

  beforeEach(() => {
    jest.resetModules();
    // JSDOM global document is provided by Jest; we ensure a minimal DOM structure.
    document.body.innerHTML = `
      <input id="username" value="Jerry" />
      <input id="password" value="securePassword123" />
    `;

    // Load jQuery bound to the JSDOM window/document.
    // eslint-disable-next-line global-require
    $ = require('jquery');
    global.$ = $;

    // Stub webgoat.customjs to avoid reference errors from jwt-refresh.js
    global.webgoat = global.webgoat || {};
    global.webgoat.customjs = global.webgoat.customjs || {};

    // Spy on $.ajax to capture login calls
    jest.spyOn($, 'ajax').mockImplementation((opts) => {
      // Immediately invoke success handler if provided
      if (opts && typeof opts.success === 'function') {
        opts.success({ access_token: 'at', refresh_token: 'rt' });
      }
      // Return a thenable-like object to satisfy any chained calls
      return {
        success: (fn) => {
          if (typeof fn === 'function') {
            fn({ access_token: 'at', refresh_token: 'rt' });
          }
        }
      };
    });

    // Finally, require the script under test so its $(document).ready handler runs.
    // eslint-disable-next-line global-require
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.$;
  });

  test('login is called (via $.ajax) with a non-empty password when #password has a value', () => {
    // The $(document).ready handler should have run on require in the Jest/JSDOM environment.
    // We now assert that $.ajax was invoked with a JSON body containing the password from #password.

    expect($.ajax).toHaveBeenCalled();

    const ajaxCall = $.ajax.mock.calls[0][0];
    expect(ajaxCall).toBeDefined();
    expect(ajaxCall.type).toBe('POST');
    expect(ajaxCall.url).toBe('JWT/refresh/login');
    expect(ajaxCall.contentType).toBe('application/json');

    const payload = JSON.parse(ajaxCall.data);
    expect(payload.user).toBe('Jerry'); // default or from #username
    expect(typeof payload.password).toBe('string');
    expect(payload.password.length).toBeGreaterThan(0);
  });

  test('implementation does not contain the original hard-coded password literal', () => {
    // Ensure the vulnerable literal "bm5nhSkxCXZkKRy4" is no longer present in the source file.
    const sourcePath = path.resolve(
      __dirname,
      '../../../../main/resources/lessons/jwt/js/jwt-refresh.js'
    );
    const sourceCode = fs.readFileSync(sourcePath, 'utf-8');

    expect(sourceCode).not.toContain('bm5nhSkxCXZkKRy4');
  });
});
