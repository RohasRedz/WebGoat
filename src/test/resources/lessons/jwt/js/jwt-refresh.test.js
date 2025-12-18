jest.mock('jquery', () => {
  const successHandlers = [];
  const ajaxMock = jest.fn((options) => {
    // Store options for later inspection; allow chaining .success(handler)
    const api = {
      success: (handler) => {
        successHandlers.push({ options, handler });
        return api;
      }
    };
    return api;
  });
  const $ = {
    ajax: ajaxMock,
    // minimal ready() implementation
    ready: (fn) => fn()
  };
  $.ajax.mock = ajaxMock;
  $.ajax._successHandlers = successHandlers;
  return $;
});

describe('jwt-refresh.js delta tests', () => {
  let $;

  beforeEach(() => {
    jest.resetModules();
    $ = require('jquery');
    $.ajax.mock.mockClear();
    $.ajax._successHandlers.length = 0;

    // Reset global config and localStorage
    global.window = global.window || {};
    window.WEBGOAT_CONFIG = { JWT_DEMO_PASSWORD: 'config-secret' };

    const store = {};
    global.localStorage = {
      getItem: (k) => store[k] || null,
      setItem: (k, v) => { store[k] = String(v); }
    };

    // Load the module under test after globals are prepared
    require('../../../../lessons/jwt/js/jwt-refresh.js'); // TODO: Adjust path if necessary
  });

  test('login uses password from configuration instead of hard-coded literal', () => {
    // Arrange
    // login('Jerry') is invoked on document.ready from the module itself
    expect($.ajax.mock).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.mock.calls[0][0];

    // Assert: request is still POSTing to the same endpoint
    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/login');

    // Assert: payload password comes from configuration, not a hard-coded string
    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('config-secret');
  });

  test('newToken stores tokens from server response, not from undeclared globals', () => {
    // Arrange
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    const { newToken } = require('../../../../lessons/jwt/js/jwt-refresh.js'); // TODO: Adjust path

    // Act
    newToken();

    // There should now be a second AJAX call
    expect($.ajax.mock).toHaveBeenCalledTimes(2);
    const call = $.ajax.mock.mock.calls[1][0];

    // Assert: second call is the token refresh POST
    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/newToken');

    const body = JSON.parse(call.data);
    expect(body.refreshToken).toBe('old-refresh');

    // Simulate successful response from server
    const handlerEntry = $.ajax._successHandlers.find(
      ({ options }) => options.url === 'JWT/refresh/newToken'
    );
    expect(handlerEntry).toBeDefined();

    handlerEntry.handler({
      access_token: 'new-access',
      refresh_token: 'new-refresh'
    });

    // Assert: localStorage updated from response, not from undeclared variables
    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
