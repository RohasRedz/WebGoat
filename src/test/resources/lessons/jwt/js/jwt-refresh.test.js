// Jest delta tests for jwt-refresh.js
// Focus: only the changed behavior related to removal of hard-coded password
// and improved token refresh handling.
// TODO: Adjust require path to match actual project structure.
describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Reset globals for each test
    global.$ = {
      ajax: jest.fn().mockReturnValue({
        success: function (cb) {
          // For tests that need success callback chaining, tests can wrap/override.
          this._successCallback = cb;
          return this;
        },
      }),
    };

    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => {
          store[k] = String(v);
        },
        clear: () => {
          store = {};
        },
      };
    })();

    global.webgoat = { customjs: {} };
    global.window = global;
    global.webgoatConfig = undefined;

    jest.resetModules();
  });

  test('login uses password from webgoatConfig and not a hard-coded literal', () => {
    // Arrange
    global.webgoatConfig = { jwtDemoPassword: 'dynamic-secret' };
    jest.doMock(
      '../../../../main/resources/lessons/jwt/js/jwt-refresh.js',
      () => {
        // Load the real script content by requiring it once mocks are in place.
        // In a real project, this would simply require the actual module file.
        return require.requireActual(
          '../../../../main/resources/lessons/jwt/js/jwt-refresh.js'
        );
      },
      { virtual: true }
    );

    // Require the script to register login() in global scope
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxArgs = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxArgs.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('dynamic-secret');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4'); // ensure old hard-coded value is not used
  });

  test('newToken updates tokens from server response instead of undeclared variables', () => {
    // Arrange
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    let capturedSuccess;
    $.ajax = jest.fn().mockReturnValue({
      success: function (cb) {
        capturedSuccess = cb;
        return this;
      },
    });

    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    global.newToken();

    // Simulate server response with new tokens
    capturedSuccess({
      access_token: 'new-access',
      refresh_token: 'new-refresh',
    });

    // Assert
    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });

  test('addBearerToken does not set Authorization header when no access_token is present', () => {
    // Arrange
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    localStorage.clear();

    // Act
    const headers = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers).toEqual({});
  });

  test('addBearerToken sets Authorization header when access_token is present', () => {
    // Arrange
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    localStorage.setItem('access_token', 'abc123');

    // Act
    const headers = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBe('Bearer abc123');
  });
});
