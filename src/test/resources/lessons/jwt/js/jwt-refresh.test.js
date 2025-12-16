// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Jest delta tests focusing on the changed behavior in jwt-refresh.js:
// - Removal of real hard-coded password, replaced by a non-secret placeholder.
// - Token handling still behaves correctly for access/refresh tokens.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      // Simulate immediate async success; caller can override by re-mocking $.ajax
      ajaxMock._successCallback = cb;
      return { success: ajaxMock };
    }
  }));
  return {
    __esModule: true,
    default: {
      ajax: ajaxMock
    },
    ajax: ajaxMock
  };
});

// NOTE: The application likely exposes `webgoat` and document/DOM globals.
// We create minimal shims required for testing the changed logic.
global.webgoat = { customjs: {} };
global.document = { readyState: 'complete' };

// localStorage mock for token behavior verification
const localStorageMock = (() => {
  let store = {};
  return {
    getItem: (key) => store[key] || null,
    setItem: (key, value) => {
      store[key] = String(value);
    },
    clear: () => {
      store = {};
    }
  };
})();
Object.defineProperty(global, 'localStorage', {
  value: localStorageMock
});

describe('jwt-refresh.js delta tests', () => {
  let $ajax;

  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
    localStorage.clear();

    // Re-require jquery mock for fresh reference
    const jq = require('jquery');
    $ajax = jq.ajax;

    // Ensure webgoat.customjs is present for the script under test
    global.webgoat = { customjs: {} };

    // Load the updated script once per test to bind functions into global scope
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  test('uses a non-secret placeholder instead of original hard-coded password', () => {
    // Arrange
    // Re-require script to get a fresh instance; login will be in scope
    const script = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    expect(typeof script).toBe('object'); // defensive: module shape may vary

    // Act: call login directly
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    expect($ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $ajax.mock.calls[0][0];

    expect(ajaxConfig.url).toBe('JWT/refresh/login');
    expect(ajaxConfig.type).toBe('POST');

    const body = JSON.parse(ajaxConfig.data);
    expect(body.user).toBe('Jerry');
    // Ensure the password is no longer the original real-looking value
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
    // And ensure it is clearly a demo/placeholder style value
    expect(typeof body.password).toBe('string');
    expect(body.password.toLowerCase()).toContain('demo');
  });

  test('login still stores access and refresh tokens from response', () => {
    // Arrange
    // eslint-disable-next-line no-undef
    login('Jerry');

    const ajaxConfig = $ajax.mock.calls[0][0];

    // Simulate server response structure
    const fakeResponse = {
      access_token: 'ACCESS123',
      refresh_token: 'REFRESH456'
    };

    // Act
    ajaxConfig.success(fakeResponse);

    // Assert
    expect(localStorage.getItem('access_token')).toBe('ACCESS123');
    expect(localStorage.getItem('refresh_token')).toBe('REFRESH456');
  });

  test('newToken uses addBearerToken header and updates tokens from response', () => {
    // Arrange
    localStorage.setItem('access_token', 'OLD_ACCESS');
    localStorage.setItem('refresh_token', 'OLD_REFRESH');

    // eslint-disable-next-line no-undef
    newToken();

    const ajaxConfig = $ajax.mock.calls[0][0];

    // Assert: Authorization header is created via webgoat.customjs.addBearerToken
    expect(typeof ajaxConfig.headers.Authorization).toBe('string');
    expect(ajaxConfig.headers.Authorization).toBe('Bearer OLD_ACCESS');
    expect(ajaxConfig.url).toBe('JWT/refresh/newToken');
    const body = JSON.parse(ajaxConfig.data);
    expect(body.refreshToken).toBe('OLD_REFRESH');

    // Act: simulate server returning refreshed tokens
    const fakeResponse = {
      access_token: 'NEW_ACCESS',
      refresh_token: 'NEW_REFRESH'
    };
    ajaxConfig.success(fakeResponse);

    // Assert
    expect(localStorage.getItem('access_token')).toBe('NEW_ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
  });
});
