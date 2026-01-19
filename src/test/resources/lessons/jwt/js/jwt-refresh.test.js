// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on the changed security behavior:
 * - Ensure there is no hard-coded password literal in the request payload.
 * - Ensure login() uses a configurable password source.
 */

const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

describe('jwt-refresh delta tests', () => {
  let window;
  let document;
  let $;

  beforeEach(() => {
    const dom = new JSDOM(`<!DOCTYPE html><html><body></body></html>`, {
      url: 'http://localhost/',
    });
    window = dom.window;
    document = window.document;
    global.window = window;
    global.document = document;

    $ = require('jquery')(window);

    global.$ = $;
    global.jQuery = $;
    global.webgoat = {
      config: { jwtPassword: 'CONFIGURED_SECRET' },
      customjs: {},
    };

    jest.spyOn($, 'ajax').mockImplementation((options) => {
      const success = options.success || options.complete || function () {};
      success({
        access_token: 'ACCESS',
        refresh_token: 'REFRESH',
      });
      return { success: (cb) => cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' }) };
    });

    const scriptPath = path.resolve(
      __dirname,
      '../../../main/resources/lessons/jwt/js/jwt-refresh.js'
    );
    const scriptContent = fs.readFileSync(scriptPath, 'utf8');
    eval(scriptContent);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.window;
    delete global.document;
    delete global.$;
    delete global.jQuery;
    delete global.webgoat;
  });

  test('login uses configured password and not hard-coded literal', () => {
    const hardcodedSecret = 'bm5nhSkxCXZkKRy4';

    const ajaxCall = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxCall.data);

    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('CONFIGURED_SECRET');
    expect(payload.password).not.toBe(hardcodedSecret);
  });

  test('newToken uses refresh_token from localStorage and updates tokens from response', () => {
    window.localStorage.setItem('access_token', 'OLD_ACCESS');
    window.localStorage.setItem('refresh_token', 'OLD_REFRESH');

    global.newToken();

    expect(window.localStorage.getItem('access_token')).toBe('ACCESS');
    expect(window.localStorage.getItem('refresh_token')).toBe('REFRESH');
  });
});
