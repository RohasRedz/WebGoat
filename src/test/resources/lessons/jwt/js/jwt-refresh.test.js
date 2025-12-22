/**
 * Delta tests for jwt-refresh.js focusing on changed behavior:
 * - login() must abort when no password is configured via #jwt-login-config data attribute.
 * - login() must proceed (issue AJAX request) when the password is properly configured.
 */

describe('jwt-refresh (delta tests)', () => {
  let $ajaxMock;
  let $;

  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();

    // Mock jQuery with minimal features: ready, ajax, and a simple selector
    $ajaxMock = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'AT', refresh_token: 'RT' }) });

    $ = function (selector) {
      if (selector === '#jwt-login-config') {
        return {
          data: (key) => {
            if (key === 'jwtPassword') {
              return $.mockPassword;
            }
            return undefined;
          }
        };
      }
      // document.ready handler
      if (typeof selector === 'function') {
        selector();
      }
      return {};
    };
    $.ajax = $ajaxMock;
    $.mockPassword = undefined;

    global.$ = $;
    global.jQuery = $;

    // Mock localStorage for tokens (focus is on behavior, not persistence)
    global.localStorage = {
      store: {},
      setItem(key, value) {
        this.store[key] = value;
      },
      getItem(key) {
        return this.store[key];
      }
    };

    // Mock console.error to assert secure failure behavior
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    delete global.$;
    delete global.jQuery;
    delete global.localStorage;
    jest.restoreAllMocks();
  });

  function loadModule() {
    // Require the module after globals are set so it picks up our mocks
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  }

  test('login aborts and does not call AJAX when password is not configured', () => {
    // Arrange: no password configured
    $.mockPassword = undefined;

    loadModule();

    // The module calls login('Jerry') on document ready; with no password,
    // it should log an error and not call $.ajax.
    expect($ajaxMock).not.toHaveBeenCalled();
    expect(console.error).toHaveBeenCalledWith(
      'JWT login password is not configured. Aborting login call.'
    );
  });

  test('login proceeds and calls AJAX when password is configured', () => {
    // Arrange: configure password via data attribute
    $.mockPassword = 'configured-secret';

    loadModule();

    // The module should have called $.ajax with the password read from configuration
    expect($ajaxMock).toHaveBeenCalledTimes(1);
    const callArgs = $ajaxMock.mock.calls[0][0];

    expect(callArgs.type).toBe('POST');
    expect(callArgs.url).toBe('JWT/refresh/login');
    expect(callArgs.contentType).toBe('application/json');

    const payload = JSON.parse(callArgs.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('configured-secret');

    // Ensure tokens are still set as side effect of success callback
    expect(global.localStorage.getItem('access_token')).toBe('AT');
    expect(global.localStorage.getItem('refresh_token')).toBe('RT');
  });
});
