/**
 * Delta tests for jwt-refresh.js focusing on:
 * - Removal of hard-coded password from the login request.
 * - Correct token storage using response data.
 * - Safe usage of localStorage accessors.
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Save callback for later invocation in tests
      ajaxMock._lastSuccess = cb;
      return this;
    },
  }));
  const $mock = function () {};
  $mock.ajax = ajaxMock;
  $mock._ajax = ajaxMock;
  $mock.fn = $mock.prototype;
  $mock.fn.ready = jest.fn((cb) => cb());
  return $mock;
});

const $ = require('jquery');

describe('jwt-refresh delta tests (no hard-coded password, secure token handling)', () => {
  let originalDocument;
  let originalMeta;
  let originalLocalStorage;
  let webgoatOriginal;

  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();

    originalDocument = global.document;
    originalMeta = {
      content: 'from-meta-secret',
    };
    global.document = {
      querySelector: jest.fn((selector) => {
        if (selector === 'meta[name="webgoat-jwt-demo-password"]') {
          return originalMeta;
        }
        return null;
      }),
    };

    originalLocalStorage = global.localStorage;
    const store = {};
    global.localStorage = {
      getItem: jest.fn((k) => store[k] || null),
      setItem: jest.fn((k, v) => {
        store[k] = v;
      }),
    };

    webgoatOriginal = global.webgoat;
    global.webgoat = { customjs: {} };

    // Load the fixed script under test
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    global.document = originalDocument;
    global.localStorage = originalLocalStorage;
    global.webgoat = webgoatOriginal;
  });

  test('login uses password from meta tag, not hard-coded in source', () => {
    // Arrange
    const ajaxMock = $.ajax._ajax;

    // Trigger document-ready, which calls login(DEFAULT_USER)
    // already executed in beforeEach by ready() stub.

    // Act
    const ajaxCallArgs = ajaxMock.mock.calls[0][0];

    // Assert
    expect(ajaxCallArgs.url).toBe('JWT/refresh/login');
    const body = JSON.parse(ajaxCallArgs.data);
    expect(body.user).toBe('Jerry'); // DEFAULT_USER behavior preserved
    expect(body.password).toBe('from-meta-secret'); // taken from meta, not hard-coded
  });

  test('tokens from server response are persisted via safeSetLocalStorageItem', () => {
    const ajaxMock = $.ajax._ajax;

    // Simulate server response using stored success callback
    const successCb = ajaxMock._lastSuccess;
    expect(typeof successCb).toBe('function');

    successCb({
      access_token: 'access123',
      refresh_token: 'refresh456',
    });

    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'access_token',
      'access123'
    );
    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'refresh_token',
      'refresh456'
    );
  });

  test('addBearerToken returns header only when access_token is present', () => {
    global.localStorage.getItem.mockImplementation((k) =>
      k === 'access_token' ? 'token-xyz' : null
    );

    const headers = global.webgoat.customjs.addBearerToken();

    expect(headers.Authorization).toBe('Bearer token-xyz');
  });

  test('newToken does nothing when no refresh_token is present', () => {
    const ajaxMock = $.ajax._ajax;
    global.localStorage.getItem.mockReturnValue(null);

    global.webgoat.customjs.newToken();

    expect(ajaxMock).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: 'JWT/refresh/newToken' })
    );
  });

  test('newToken sends stored tokens and updates them from response', () => {
    const ajaxMock = $.ajax._ajax;
    const store = {
      access_token: 'old-access',
      refresh_token: 'old-refresh',
    };
    global.localStorage.getItem.mockImplementation((k) => store[k] || null);

    global.webgoat.customjs.newToken();

    const call = ajaxMock.mock.calls.find(
      (c) => c[0].url === 'JWT/refresh/newToken'
    );
    expect(call).toBeDefined();

    const config = call[0];
    expect(config.headers.Authorization).toBe('Bearer old-access');

    const payload = JSON.parse(config.data);
    expect(payload.refreshToken).toBe('old-refresh');

    // Simulate success callback for newToken request
    const successCb = ajaxMock._lastSuccess;
    successCb({
      access_token: 'new-access',
      refresh_token: 'new-refresh',
    });

    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'access_token',
      'new-access'
    );
    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'refresh_token',
      'new-refresh'
    );
  });
});
