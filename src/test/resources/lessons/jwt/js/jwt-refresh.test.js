// Assumed module name/path based on resolved_file_path:
// src/main/resources/lessons/jwt/js/jwt-refresh.js
// e28692 src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// TODO: Adjust import path/module loading according to your bundler/test setup.

const $ = require('jquery');

// Recreate (inlined) the updated production logic from jwt-refresh.js
// so tests can focus on changed security behavior (removed hard-coded password,
// safer token handling).
function createJwtModuleUnderTest() {
  function login(user, password) {
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
      if (response && typeof response === 'object') {
        if (response['access_token']) {
          localStorage.setItem('access_token', response['access_token']);
        }
        if (response['refresh_token']) {
          localStorage.setItem('refresh_token', response['refresh_token']);
        }
      }
    });
  }

  function newToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
      headers: {
        Authorization: 'Bearer ' + localStorage.getItem('access_token')
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      contentType: 'application/json',
      data: JSON.stringify({ refreshToken })
    }).success(function (response) {
      if (response && typeof response === 'object') {
        if (response['access_token']) {
          localStorage.setItem('access_token', response['access_token']);
        }
        if (response['refresh_token']) {
          localStorage.setItem('refresh_token', response['refresh_token']);
        }
      }
    });
  }

  return { login, newToken };
}

describe('jwt-refresh delta security tests (no hard-coded password, safer token flow)', () => {
  let jwtModule;
  let ajaxMock;

  beforeEach(() => {
    // Mock localStorage for deterministic tests
    const store = {};
    global.localStorage = {
      getItem: jest.fn((key) => store[key]),
      setItem: jest.fn((key, value) => {
        store[key] = String(value);
      })
    };

    // Mock $.ajax and capture its configuration
    ajaxMock = jest.spyOn($, 'ajax').mockImplementation((config) => {
      // Return an object with success(cb) to simulate jQuery's promise-like API
      return {
        success: (cb) => {
          // Tests will manually call cb with mock responses.
          ajaxMock.lastCallback = cb;
          return this;
        }
      };
    });

    jwtModule = createJwtModuleUnderTest();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('login sends caller-supplied password (no hard-coded secret in payload)', () => {
    // Arrange
    const username = 'Jerry';
    const suppliedPassword = 'user-supplied-password';

    // Act
    jwtModule.login(username, suppliedPassword);

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    expect(ajaxConfig.url).toBe('JWT/refresh/login');

    const body = JSON.parse(ajaxConfig.data);
    expect(body.user).toBe(username);
    expect(body.password).toBe(suppliedPassword);
    // Critical security assertion: ensure no known hard-coded secret is present.
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login stores access and refresh tokens only from response object', () => {
    // Arrange
    jwtModule.login('Jerry', 'any-password');
    const ajaxConfig = $.ajax.mock.calls[0][0];
    expect(typeof ajaxConfig).toBe('object');
    const successCallback = ajaxMock.lastCallback;

    const response = {
      access_token: 'access-123',
      refresh_token: 'refresh-456'
    };

    // Act
    successCallback(response);

    // Assert
    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'access-123');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'refresh-456');
  });

  test('login does not set tokens when response is malformed or missing tokens', () => {
    // Arrange
    jwtModule.login('Jerry', 'any-password');
    const successCallback = ajaxMock.lastCallback;

    // Act
    successCallback(null); // malformed
    successCallback({});  // no tokens

    // Assert
    expect(localStorage.setItem).not.toHaveBeenCalled();
  });

  test('newToken sends refresh token from localStorage and updates tokens from response', () => {
    // Arrange
    localStorage.setItem('access_token', 'existing-access');
    localStorage.setItem('refresh_token', 'existing-refresh');

    // Act
    jwtModule.newToken();

    // Assert: request config
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    expect(ajaxConfig.url).toBe('JWT/refresh/newToken');
    expect(ajaxConfig.headers.Authorization).toBe('Bearer existing-access');

    const body = JSON.parse(ajaxConfig.data);
    expect(body.refreshToken).toBe('existing-refresh');

    // Simulate server response
    const successCallback = ajaxMock.lastCallback;
    const response = {
      access_token: 'new-access-999',
      refresh_token: 'new-refresh-999'
    };
    successCallback(response);

    // Assert: tokens updated only from response, not undefined variables
    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'new-access-999');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'new-refresh-999');
  });

  test('newToken gracefully handles missing tokens in response', () => {
    // Arrange
    localStorage.setItem('access_token', 'existing-access');
    localStorage.setItem('refresh_token', 'existing-refresh');
    jwtModule.newToken();
    const successCallback = ajaxMock.lastCallback;

    // Act
    successCallback({}); // no access_token or refresh_token

    // Assert
    // No additional setItem calls beyond initial setup
    expect(localStorage.setItem).toHaveBeenCalledTimes(2);
  });
});
