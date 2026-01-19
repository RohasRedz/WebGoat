// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta test for jwt-refresh.js to verify removal of hard-coded password
// and correct usage of getLessonPassword() in the login flow.

const { JSDOM } = require('jsdom');

// Load jQuery and set up a basic DOM environment for the script
let dom;
let $;

beforeEach(() => {
  dom = new JSDOM(`<!doctype html><html><body>
      <div id="jwt-refresh-config" data-password="dynamicSecret123"></div>
    </body></html>`, {
    url: 'http://example.com'
  });
  global.window = dom.window;
  global.document = dom.window.document;
  $ = require('jquery')(dom.window);
  global.$ = $;

  // Provide the webgoat namespace with customjs as used in the script
  global.webgoat = { customjs: {} };
});

afterEach(() => {
  // Cleanup globals
  delete global.window;
  delete global.document;
  delete global.$;
  delete global.webgoat;
});

describe('jwt-refresh login and getLessonPassword behavior (hard-coded password fix)', () => {
  test('login uses getLessonPassword and no hard-coded password literal is present', () => {
    // Spy on $.ajax to capture payload
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((options) => {
      // Simulate a successful request, invoking success callback
      if (typeof options.success === 'function') {
        options.success({ access_token: 'access', refresh_token: 'refresh' });
      } else if (typeof options.then === 'function') {
        options.then({ access_token: 'access', refresh_token: 'refresh' });
      }
      return { success: (cb) => cb({ access_token: 'access', refresh_token: 'refresh' }) };
    });

    // Require the updated script; it will define login and getLessonPassword in global scope
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Ensure getLessonPassword is available and returns the DOM-configured password
    expect(typeof global.getLessonPassword).toBe('function');
    const password = global.getLessonPassword();
    expect(password).toBe('dynamicSecret123');

    // Call login, which should internally call getLessonPassword() and use its result
    global.login('Jerry');

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(body.user).toBe('Jerry');
    // Crucial delta assertion: password comes from getLessonPassword and is not a hard-coded literal
    expect(body.password).toBe('dynamicSecret123');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');

    ajaxSpy.mockRestore();
  });

  test('getLessonPassword falls back to a non-sensitive placeholder when no DOM config present', () => {
    // Remove the config element so that fallback path is exercised
    const configEl = document.getElementById('jwt-refresh-config');
    configEl.parentNode.removeChild(configEl);

    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const password = global.getLessonPassword();
    // Verifies the non-hard-coded, clearly placeholder nature
    expect(password).toBe('CHANGE_ME_LESSON_PASSWORD');
  });
});
