// Jest tests for jwt-refresh.js
// Test file path (derived from source): src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// We require the script under test after setting up globals it depends on.
describe('jwt-refresh.js delta tests', () => {
  let originalWebgoat;
  let originalLocalStorage;
  let ajaxMock;

  beforeEach(() => {
    // Mock global webgoat and config
    originalWebgoat = global.webgoat;
    global.webgoat = {
      config: {
        jwtDemoPassword: 'configured-secret'
      },
      customjs: {}
    };

    // Mock localStorage
    originalLocalStorage = global.localStorage;
    const storage = {};
    global.localStorage = {
      setItem: (k, v) => {
        storage[k] = String(v);
      },
      getItem: (k) => storage[k]
    };

    // Mock jQuery and $.ajax
    ajaxMock = jest.fn().mockReturnValue({
      success: (cb) => {
        cb({ access_token: 'acc', refresh_token: 'ref' });
        return { success: () => {} };
      }
    });

    global.$ = {
      ajax: ajaxMock,
      // $(document).ready(...) is used – simulate it by calling the callback immediately
      ready: (cb) => cb()
    };

    // Load script under test (assumes CommonJS or test bundler that maps path correctly)
    // eslint-disable-next-line global-require
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    jest.resetModules();
    global.webgoat = originalWebgoat;
    global.localStorage = originalLocalStorage;
    delete global.$;
  });

  test('login uses configured password and does not contain the old hard-coded password', () => {
    // The script calls login('Jerry') on document ready in its top-level code.
    expect(ajaxMock).toHaveBeenCalledTimes(1);

    const ajaxCallArg = ajaxMock.mock.calls[0][0];
    expect(ajaxCallArg.type).toBe('POST');
    expect(ajaxCallArg.url).toBe('JWT/refresh/login');

    const body = JSON.parse(ajaxCallArg.data);
    // Assert: old hard-coded password is not used
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
    // Assert: configured password is used instead
    expect(body.password).toBe('configured-secret');
  });

  test('login falls back to empty password when configuration is missing', () => {
    jest.resetModules();
    ajaxMock.mockClear();

    // Remove config and reload script
    global.webgoat = { customjs: {} };
    // eslint-disable-next-line global-require
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxCallArg = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(ajaxCallArg.data);

    expect(body.password).toBe('');
  });
});
