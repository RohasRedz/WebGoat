/**
 * NOTE: These tests assume the updated jwt-refresh.js uses getConfiguredPassword()
 * and reads window.webgoat.config.jwtRefreshPassword when present.
 */

describe('jwt-refresh security fixes', () => {
  let originalWebgoat;
  let $ajaxSpy;

  beforeEach(() => {
    originalWebgoat = global.webgoat;
    global.webgoat = {
      customjs: {},
      config: {}
    };

    $ajaxSpy = jest.fn().mockReturnValue({
      success: (cb) => {
        cb({ access_token: 'access', refresh_token: 'refresh' });
      }
    });

    global.$ = { ajax: $ajaxSpy };
    global.localStorage = {
      store: {},
      setItem(key, value) { this.store[key] = value; },
      getItem(key) { return this.store[key]; }
    };

    // Load module under test after globals are prepared
    jest.resetModules();
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    jest.resetModules();
  });

  test('getConfiguredPassword returns configured value when present', () => {
    webgoat.config.jwtRefreshPassword = 'configured-secret';

    // getConfiguredPassword is not exported, but login uses it internally.
    // We verify via the AJAX payload that the configured value is used.
    jest.resetModules();
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // trigger login
    const callArgs = $ajaxSpy.mock.calls[0][0];
    const data = JSON.parse(callArgs.data);

    expect(data.password).toBe('configured-secret');
  });

  test('getConfiguredPassword falls back to empty string when config missing', () => {
    delete webgoat.config.jwtRefreshPassword;

    jest.resetModules();
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const callArgs = $ajaxSpy.mock.calls[0][0];
    const data = JSON.parse(callArgs.data);

    expect(data.password).toBe('');
  });

  test('login uses password value from configuration accessor (no hard-coded literal)', () => {
    webgoat.config.jwtRefreshPassword = 'dynamic-secret';

    jest.resetModules();
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const callArgs = $ajaxSpy.mock.calls[0][0];
    const data = JSON.parse(callArgs.data);

    expect(data.password).toBe('dynamic-secret');
  });
});
