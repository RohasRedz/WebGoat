// File path (derived conceptually from src/main -> src/test): src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// NOTE: Adjust import path to match your Jest/module resolution settings.

const { JSDOM } = require('jsdom');

// eslint-disable-next-line global-require
const scriptPath = '../../../../../main/resources/lessons/jwt/js/jwt-refresh.js';

describe('jwt-refresh hard-coded password removal (delta tests)', () => {
  let originalAjax;

  beforeEach(() => {
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/'
    });
    global.window = dom.window;
    global.document = dom.window.document;
    global.localStorage = dom.window.localStorage;

    // Provide the global webgoat object used by the script
    global.webgoat = { customjs: {} };

    // Mock jQuery and its ajax API
    originalAjax = jest.fn().mockReturnValue({
      success: (cb) => {
        cb({ access_token: 'tokenA', refresh_token: 'tokenR' });
      }
    });

    global.$ = {
      ajax: originalAjax
    };

    // Now require the script so that it registers functions into the global scope.
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require, import/no-dynamic-require
      require(scriptPath);
    });
  });

  afterEach(() => {
    jest.resetModules();
  });

  test('login uses configured password and no longer hardcodes secret literal', () => {
    // Arrange: configure a password in the global webgoat configuration
    global.webgoat.jwt = {
      loginPassword: 'CONFIGURED_SECRET'
    };

    // Call the globally defined login function
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert: ajax was called with the configured password, not a hard-coded value.
    expect(originalAjax).toHaveBeenCalledTimes(1);
    const ajaxConfig = originalAjax.mock.calls[0][0];

    const body = JSON.parse(ajaxConfig.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('CONFIGURED_SECRET');

    // Ensure that the specific old literal is not present anywhere in the payload
    expect(ajaxConfig.data).not.toContain('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to non-secret password when configuration is absent', () => {
    // Remove any jwt configuration
    delete global.webgoat.jwt;

    // eslint-disable-next-line no-undef
    login('Jerry');

    expect(originalAjax).toHaveBeenCalledTimes(1);
    const ajaxConfig = originalAjax.mock.calls[0][0];

    const body = JSON.parse(ajaxConfig.data);
    expect(body.user).toBe('Jerry');
    // Fallback in code is empty string; this ensures no static secret is used by default.
    expect(body.password).toBe('');
  });

  test('addBearerToken reads token only from localStorage and not from URL or hard-coded value', () => {
    // Arrange
    global.localStorage.setItem('access_token', 'ACCESS_TOKEN_VALUE');

    // eslint-disable-next-line no-undef
    const headers = webgoat.customjs.addBearerToken();

    expect(headers.Authorization).toBe('Bearer ACCESS_TOKEN_VALUE');
  });
});
