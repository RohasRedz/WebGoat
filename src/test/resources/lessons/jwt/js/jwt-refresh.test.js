/**
 * @file src/test/resources/lessons/jwt/js/jwt-refresh.test.js
 *
 * Delta Jest tests for jwt-refresh.js focusing on:
 *  - removal of hard-coded password from login request
 *  - correct usage of refreshed tokens from server response
 */

describe('jwt-refresh (delta tests)', () => {
  let $ajaxMock;
  let originalWebgoat;

  beforeEach(() => {
    // Mock jQuery.ajax
    $ajaxMock = jest.fn().mockReturnValue({ success: function (cb) { this._success = cb; return this; } });
    global.$ = {
      ajax: $ajaxMock
    };

    // Mock localStorage
    const store = {};
    global.localStorage = {
      getItem: jest.fn((k) => store[k]),
      setItem: jest.fn((k, v) => { store[k] = v; })
    };

    // Mock webgoat.customjs namespace used in file
    originalWebgoat = global.webgoat;
    global.webgoat = { customjs: {} };

    // Ensure document.ready handler does not automatically run during require:
    // We simulate jQuery ready as a no-op.
    global.$.ready = (fn) => fn && fn();

    // Provide a runtime password to avoid falling back to empty string
    global.WEBGOAT_JWT_PASSWORD = 'runtimePassword';

    // Require the updated script under test
    jest.isolateModules(() => {
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    delete global.WEBGOAT_JWT_PASSWORD;
  });

  test('login sends password from runtime configuration, not hard-coded value', () => {
    // Arrange
    // First ajax call should be for JWT/refresh/login triggered on document.ready
    const firstCallConfig = $ajaxMock.mock.calls[0][0];

    // Assert
    expect(firstCallConfig.url).toBe('JWT/refresh/login');
    const body = JSON.parse(firstCallConfig.data);

    // The password must come from WEBGOAT_JWT_PASSWORD, not from a hard-coded literal
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtimePassword');
  });

  test('newToken uses refreshed tokens from server response', () => {
    // Arrange
    // Find the function newToken attached to global scope (defined by script)
    const newToken = global.newToken;
    expect(typeof newToken).toBe('function');

    localStorage.getItem.mockImplementation((key) => {
      if (key === 'access_token') return 'oldAccess';
      if (key === 'refresh_token') return 'oldRefresh';
      return null;
    });

    // Act
    newToken();

    // Capture the ajax call for newToken (should be second call)
    const newTokenCallConfig = $ajaxMock.mock.calls[1][0];
    // Simulate server response through success callback
    const response = { access_token: 'newAccess', refresh_token: 'newRefresh' };
    if (typeof $ajaxMock.mock.results[1].value._success === 'function') {
      $ajaxMock.mock.results[1].value._success(response);
    }

    // Assert
    expect(newTokenCallConfig.url).toBe('JWT/refresh/newToken');
    const body = JSON.parse(newTokenCallConfig.data);
    expect(body.refreshToken).toBe('oldRefresh');

    // Tokens in localStorage should be updated from response, not undefined variables
    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'newAccess');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'newRefresh');
  });
});
