// File path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Jest delta tests for jwt-refresh.js focusing on:
// - login(user, password) using the provided password (no hard-coded value).
// - DOM-based password retrieval in $(document).ready handler.
// - Correct token storage based on mocked AJAX response.

jest.mock('jquery', () => {
  const successMock = jest.fn(function (cb) {
    // Allow chaining: cb will be invoked by the calling test when appropriate
    this._successCallback = cb;
    return this;
  });

  const ajaxMock = jest.fn((options) => {
    const wrapper = {
      options,
      success: successMock
    };
    return wrapper;
  });

  const readyMock = jest.fn((cb) => {
    // Immediately invoke the ready callback to simulate DOM ready.
    cb();
  });

  return {
    ajax: ajaxMock,
    fn: {},
    ready: readyMock,
    // For compatibility with "$(document).ready"
    __esModule: true,
    default: {
      ajax: ajaxMock,
      fn: {},
      ready: readyMock
    }
  };
});

const $ = require('jquery');
const ajaxMock = $.ajax;

// Ensure webgoat.customjs exists for the script under test
global.webgoat = { customjs: {} };

// Mock localStorage
const localStorageMock = (() => {
  let store = {};
  return {
    getItem: jest.fn((key) => store[key]),
    setItem: jest.fn((key, value) => {
      store[key] = String(value);
    }),
    clear: jest.fn(() => {
      store = {};
    })
  };
})();

Object.defineProperty(global, 'localStorage', {
  value: localStorageMock,
  configurable: true
});

// Provide a minimal document implementation with querySelector support
let dataPasswordValue = null;
global.document = {
  querySelector: jest.fn((selector) => {
    if (selector === '[data-jwt-password]' && dataPasswordValue !== null) {
      return {
        getAttribute: () => dataPasswordValue
      };
    }
    return null;
  }),
  URL: 'http://localhost'
};

// Load the script under test after mocks are in place
require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js (delta tests)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    dataPasswordValue = null;
  });

  test('$(document).ready retrieves password from data attribute and passes it to login', () => {
    // Arrange
    dataPasswordValue = 'runtime-secret';

    // Re-require to re-trigger ready handler with current dataPasswordValue
    jest.isolateModules(() => {
      require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const call = ajaxMock.mock.calls[0][0];

    // Body should contain the password from the data attribute, not a hard-coded literal
    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('runtime-secret');
  });

  test('$(document).ready falls back to empty password when data attribute missing', () => {
    // Arrange
    dataPasswordValue = null; // no data-jwt-password element present

    jest.isolateModules(() => {
      require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const call = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(call.data);
    expect(payload.password).toBe('');
  });

  test('login(user, password) uses provided password and stores tokens from success callback', () => {
    // Arrange
    const user = 'Jerry';
    const password = 'provided-password';

    // Find login in the loaded script. It should be defined in the global scope.
    const login = global.login;
    expect(typeof login).toBe('function');

    // Act
    login(user, password);

    // Assert: password passed into AJAX call body
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const options = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(options.data);
    expect(payload.user).toBe(user);
    expect(payload.password).toBe(password);

    // Simulate server response via the stored success callback
    const wrapper = ajaxMock.mock.results[0].value;
    const successCallback = wrapper._successCallback;
    const response = {
      access_token: 'access123',
      refresh_token: 'refresh456'
    };
    successCallback(response);

    expect(localStorage.setItem).toHaveBeenCalledWith(
      'access_token', 'access123'
    );
    expect(localStorage.setItem).toHaveBeenCalledWith(
      'refresh_token', 'refresh456'
    );
  });

  test('addBearerToken returns Authorization header with token from localStorage', () => {
    // Arrange
    localStorage.setItem('access_token', 'tokenXYZ');

    // Act
    const headers = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers.Authorization).toBe('Bearer tokenXYZ');
  });
});
