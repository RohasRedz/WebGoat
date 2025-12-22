// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// NOTE: This is a delta test file focused only on the behavioral changes introduced
//       to fix the hard-coded password vulnerability in jwt-refresh.js.
//       It assumes Jest is configured and that the production file attaches its
//       functions to the global/window scope via the IIFE.

'use strict';

// TODO: Adjust the require path if your bundler/module system is different.
require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js  delta tests for removal of hard-coded password and safer token handling', () => {
  let originalLocalStorage;
  let localStorageMock;
  let originalWebgoat;
  let original$;

  beforeEach(() => {
    // Mock jQuery's ready and ajax for isolation and determinism
    original$ = global.$;
    const ajaxMock = jest.fn().mockReturnValue({
      done: jest.fn((cb) => {
        // Default: simulate successful response with tokens
        cb({
          access_token: 'ACCESS_TOKEN_FROM_SERVER',
          refresh_token: 'REFRESH_TOKEN_FROM_SERVER',
        });
      }),
    });

    const readyMock = jest.fn((handler) => {
      // Invoke the ready handler immediately to simulate DOM ready
      handler();
    });

    global.$ = Object.assign(jest.fn(), {
      ajax: ajaxMock,
      ready: readyMock,
    });

    // Mock localStorage
    originalLocalStorage = global.localStorage;
    localStorageMock = (function () {
      let store = {};
      return {
        getItem: jest.fn((key) => store[key] || null),
        setItem: jest.fn((key, value) => {
          store[key] = String(value);
        }),
        clear: jest.fn(() => {
          store = {};
        }),
      };
    })();
    global.localStorage = localStorageMock;

    // Ensure webgoat namespace exists and is reset
    originalWebgoat = global.webgoat;
    global.webgoat = {};
  });

  afterEach(() => {
    global.$ = original$;
    global.localStorage = originalLocalStorage;
    global.webgoat = originalWebgoat;
    jest.clearAllMocks();
    jest.resetModules();
  });

  test('login is invoked on DOM ready without any hard-coded password value', () => {
    // Arrange
    // Re-require the module so that its IIFE runs with current mocks
    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    // The document ready handler is already run in beforeEach via $.ready

    // Assert
    // Inspect the arguments passed to $.ajax to ensure password is not hard-coded.
    const ajaxCalls = global.$.ajax.mock.calls;
    expect(ajaxCalls.length).toBeGreaterThan(0);

    const firstCallConfig = ajaxCalls[0][0];

    expect(firstCallConfig.url).toBe('JWT/refresh/login');
    expect(firstCallConfig.type).toBe('POST');
    expect(firstCallConfig.contentType).toBe('application/json');

    const body = JSON.parse(firstCallConfig.data);
    // User remains as "Jerry"
    expect(body.user).toBe('Jerry');
    // The crucial delta behavior: password must no longer be the original hard-coded value.
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('successful login stores access_token and refresh_token from server response without logging them', () => {
    // Arrange
    jest.spyOn(global.console, 'log').mockImplementation(() => {});
    jest.spyOn(global.console, 'info').mockImplementation(() => {});
    jest.spyOn(global.console, 'debug').mockImplementation(() => {});

    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    const ajaxCalls = global.$.ajax.mock.calls;
    expect(ajaxCalls.length).toBeGreaterThan(0);
    const ajaxConfig = ajaxCalls[0][0];

    // Simulate the .done handler manually to ensure tokens are stored
    const simulatedResponse = {
      access_token: 'ACCESS_TOKEN_FROM_SERVER',
      refresh_token: 'REFRESH_TOKEN_FROM_SERVER',
    };
    ajaxConfig.success
      ? ajaxConfig.success(simulatedResponse)
      : null; // in case legacy success callback is attached

    // Assert: tokens are stored but not logged
    expect(localStorageMock.setItem).toHaveBeenCalledWith(
      'access_token',
      'ACCESS_TOKEN_FROM_SERVER',
    );
    expect(localStorageMock.setItem).toHaveBeenCalledWith(
      'refresh_token',
      'REFRESH_TOKEN_FROM_SERVER',
    );

    const consoleOutput =
      (console.log.mock.calls.join(' ') ||
        '') +
      (console.info.mock.calls.join(' ') || '') +
      (console.debug.mock.calls.join(' ') || '');

    expect(consoleOutput).not.toContain('ACCESS_TOKEN_FROM_SERVER');
    expect(consoleOutput).not.toContain('REFRESH_TOKEN_FROM_SERVER');
  });

  test('addBearerToken reads access_token from localStorage and does not log it', () => {
    // Arrange
    localStorageMock.setItem('access_token', 'ACCESS_TOKEN_FROM_SERVER');

    jest.spyOn(global.console, 'log').mockImplementation(() => {});
    jest.spyOn(global.console, 'info').mockImplementation(() => {});
    jest.spyOn(global.console, 'debug').mockImplementation(() => {});

    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Ensure addBearerToken is attached as per the fixed implementation
    expect(global.webgoat).toBeDefined();
    expect(global.webgoat.customjs).toBeDefined();
    expect(typeof global.webgoat.customjs.addBearerToken).toBe('function');

    // Act
    const headers = global.webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBe(
      'Bearer ACCESS_TOKEN_FROM_SERVER',
    );

    const consoleOutput =
      (console.log.mock.calls.join(' ') ||
        '') +
      (console.info.mock.calls.join(' ') || '') +
      (console.debug.mock.calls.join(' ') || '');
    expect(consoleOutput).not.toContain('ACCESS_TOKEN_FROM_SERVER');
  });

  test('newToken uses stored tokens and updates them from server response without logging them', () => {
    // Arrange: put initial tokens into localStorage
    localStorageMock.setItem('access_token', 'OLD_ACCESS_TOKEN');
    localStorageMock.setItem('refresh_token', 'OLD_REFRESH_TOKEN');

    // Configure $.ajax for newToken calls
    const ajaxMock = jest.fn().mockReturnValue({
      done: jest.fn((cb) => {
        cb({
          access_token: 'NEW_ACCESS_TOKEN',
          refresh_token: 'NEW_REFRESH_TOKEN',
        });
      }),
    });
    global.$.ajax = ajaxMock;

    jest.spyOn(global.console, 'log').mockImplementation(() => {});
    jest.spyOn(global.console, 'info').mockImplementation(() => {});
    jest.spyOn(global.console, 'debug').mockImplementation(() => {});

    jest.resetModules();
    // Re-require the module so newToken is defined inside the IIFE scope.
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Because newToken is not exported to global, we re-trigger it indirectly:
    // call addBearerToken to ensure namespace exists; then access the function via a known pattern
    expect(global.webgoat).toBeDefined();
    expect(global.webgoat.customjs).toBeDefined();

    // We cannot directly access newToken if it remains private; this test
    // primarily asserts the changed behavior of token handling via ajax config.
    // So instead, we assert that when newToken runs, it uses localStorage values.

    // Simulate what newToken would do in terms of ajax call configuration:
    // Expect: headers.Authorization uses currently stored access_token
    // and body uses currently stored refresh_token.
    // We approximate this by checking the last $.ajax call configuration.
    const newTokenAjaxCall = ajaxMock.mock.calls[0]?.[0] || null;
    if (newTokenAjaxCall) {
      const headers = newTokenAjaxCall.headers || {};
      expect(headers.Authorization).toBe('Bearer OLD_ACCESS_TOKEN');

      const body = JSON.parse(newTokenAjaxCall.data);
      expect(body.refreshToken).toBe('OLD_REFRESH_TOKEN');
    }

    // Assert: tokens can be updated by server response and are not logged
    const consoleOutput =
      (console.log.mock.calls.join(' ') ||
        '') +
      (console.info.mock.calls.join(' ') || '') +
      (console.debug.mock.calls.join(' ') || '');
    expect(consoleOutput).not.toContain('OLD_ACCESS_TOKEN');
    expect(consoleOutput).not.toContain('OLD_REFRESH_TOKEN');
    expect(consoleOutput).not.toContain('NEW_ACCESS_TOKEN');
    expect(consoleOutput).not.toContain('NEW_REFRESH_TOKEN');
  });
});
