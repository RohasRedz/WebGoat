const $ = require('jquery');

// Ensure global $ for the script-under-test if it relies on it.
global.$ = $;

// Mock webgoat.customjs namespace expected by jwt-refresh.js
global.webgoat = global.webgoat || {};
webgoat.customjs = webgoat.customjs || {};

// Load the updated script so that its functions are attached to global scope.
require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh delta tests', () => {
  let originalAjax;

  beforeEach(() => {
    originalAjax = $.ajax;
    $.ajax = jest.fn();
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: key => store[key] || null,
        setItem: (key, value) => {
          store[key] = String(value);
        },
        clear: () => {
          store = {};
        }
      };
    })();
  });

  afterEach(() => {
    $.ajax = originalAjax;
  });

  test('login does not send a hard-coded password and uses a non-secret placeholder instead', () => {
    // Arrange
    const user = 'Jerry';
    const captured = {};

    $.ajax.mockImplementation(opts => {
      captured.type = opts.type;
      captured.url = opts.url;
      captured.contentType = opts.contentType;
      captured.data = opts.data;

      // Simulate success callback
      if (typeof opts.success === 'function') {
        opts.success({ access_token: 'a', refresh_token: 'r' });
      } else if (typeof opts.done === 'function') {
        opts.done({ access_token: 'a', refresh_token: 'r' });
      }

      return { success: jest.fn(), done: jest.fn() };
    });

    // Act
    global.login(user);

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    expect(captured.type).toBe('POST');
    expect(captured.url).toBe('JWT/refresh/login');
    expect(captured.contentType).toBe('application/json');

    const payload = JSON.parse(captured.data);
    expect(payload.user).toBe(user);
    // Vulnerability fix: password should no longer be the hard-coded secret
    expect(payload.password).toBe('');
  });

  test('newToken uses tokens from response and not undeclared variables', () => {
    // Arrange
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    const requests = [];
    $.ajax.mockImplementation(opts => {
      requests.push(opts);

      // Simulate server returning new tokens
      if (opts && typeof opts.success === 'function') {
        opts.success({ access_token: 'new-access', refresh_token: 'new-refresh' });
      } else if (opts && typeof opts.done === 'function') {
        opts.done({ access_token: 'new-access', refresh_token: 'new-refresh' });
      }

      return { success: jest.fn(), done: jest.fn() };
    });

    // Act
    global.newToken();

    // Assert
    expect(requests).toHaveLength(1);
    const call = requests[0];

    expect(call.url).toBe('JWT/refresh/newToken');
    expect(call.type).toBe('POST');
    expect(call.headers.Authorization).toBe('Bearer old-access');

    const body = JSON.parse(call.data);
    expect(body.refreshToken).toBe('old-refresh');

    // After success, values should come from response, not from undeclared variables
    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
