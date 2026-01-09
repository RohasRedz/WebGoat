/**
 * Delta tests for jwt-refresh.js focusing on removal of hard-coded password
 * and reliance on server-provided tokens.
 */

const $ = require('jquery');

jest.mock('jquery', () => {
  const original = jest.requireActual('jquery');
  return Object.assign(function () { return original; }, original, {
    ajax: jest.fn()
  });
});

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // reset localStorage mock
    const store = {};
    global.localStorage = {
      getItem: jest.fn((k) => store[k]),
      setItem: jest.fn((k, v) => { store[k] = v; }),
      removeItem: jest.fn((k) => { delete store[k]; })
    };
    $.ajax.mockReset();
    document.body.innerHTML = '';
  });

  test('login should not send a hard-coded password literal', () => {
    // Load the script (it will call login("Jerry") on document.ready)
    jest.isolateModules(() => {
      // jsdom simulates DOM ready immediately for inline ready handlers
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh');
    });

    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];

    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/login');
    const body = JSON.parse(call.data);

    // Ensure no hard-coded secret appears
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('');
  });

  test('newToken should store tokens from server response, not hardcoded values', () => {
    // Arrange
    global.webgoat = { customjs: {} };
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh');

    const ajaxImpl = (options) => {
      const success = options.success || options.success;
      if (typeof success === 'function') {
        success({
          access_token: 'server_access',
          refresh_token: 'server_refresh'
        });
      }
      return { success: jest.fn() };
    };
    $.ajax.mockImplementation(ajaxImpl);

    // Act
    // Call newToken directly from global scope
    global.newToken();

    // Assert
    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'server_access');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'server_refresh');
  });
});
