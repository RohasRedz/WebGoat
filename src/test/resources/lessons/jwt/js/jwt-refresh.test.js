// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests for jwt-refresh.js focusing on removal of hardcoded password and token handling behavior.

describe('jwt-refresh login flow (delta tests)', () => {
  let originalConfig;
  let $ajaxSpy;

  beforeEach(() => {
    // Mock global configuration object used by jwt-refresh.js
    originalConfig = global.WEBGOAT_JWT_CONFIG;
    global.WEBGOAT_JWT_CONFIG = { password: 'runtimeSecret' };

    // Mock localStorage
    const storage = {};
    global.localStorage = {
      getItem: jest.fn((k) => storage[k]),
      setItem: jest.fn((k, v) => {
        storage[k] = String(v);
      })
    };

    // Mock console
    global.console = {
      error: jest.fn(),
      log: jest.fn()
    };

    // Mock jQuery.ajax
    $ajaxSpy = jest.fn().mockReturnValue({
      done: function (cb) {
        cb({ access_token: 'access123', refresh_token: 'refresh123' });
      }
    });
    global.$ = {
      ajax: $ajaxSpy
    };

    // Mock webgoat namespace used later in the file
    global.webgoat = { customjs: {} };

    // Mock document.ready execution: we will manually call login via the module under test
    global.document = { readyState: 'complete' };
  });

  afterEach(() => {
    global.WEBGOAT_JWT_CONFIG = originalConfig;
    jest.resetModules();
  });

  test('login uses non-hardcoded password from WEBGOAT_JWT_CONFIG and stores tokens', () => {
    // Arrange
    // Load module after globals are prepared
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // The script calls login('Jerry') on document ready; our spies capture the behavior.

    // Assert: ajax called with password from config, not hardcoded string
    expect($ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxArg = $ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(ajaxArg.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtimeSecret');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');

    // Tokens from response should be stored in localStorage
    expect(global.localStorage.setItem).toHaveBeenCalledWith('access_token', 'access123');
    expect(global.localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'refresh123');
  });

  test('login aborts and logs error when password configuration is missing', () => {
    // Arrange
    global.WEBGOAT_JWT_CONFIG = {}; // no password configured
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // Assert
    expect($ajaxSpy).not.toHaveBeenCalled();
    expect(global.console.error).toHaveBeenCalledWith(
      'JWT login aborted due to missing secure password configuration.'
    );
  });

  test('addBearerToken sets Authorization header only when access_token is present', () => {
    // Arrange
    global.WEBGOAT_JWT_CONFIG = { password: 'runtimeSecret' };
    // prime localStorage
    global.localStorage.getItem.mockImplementation((key) => {
      if (key === 'access_token') {
        return 'token-value';
      }
      return null;
    });

    let headers;
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
      headers = global.webgoat.customjs.addBearerToken();
    });

    // Assert
    expect(headers.Authorization).toBe('Bearer token-value');
  });
});
