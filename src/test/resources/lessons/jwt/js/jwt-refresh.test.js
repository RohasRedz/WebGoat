// TODO: Adjust path if project structure differs; this assumes Jest is resolving this file directly.
const $ = require('jquery');

jest.mock('jquery', () => {
  const original = jest.requireActual('jquery');
  const mockAjax = jest.fn(() => ({
    success: function (cb) {
      mockAjax._successCallback = cb;
      return this;
    },
  }));
  const wrapped = (...args) => original(...args);
  wrapped.ajax = mockAjax;
  wrapped._mockAjax = mockAjax;
  return wrapped;
});

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Reset mocks and localStorage
    $.ajax._mockAjax.mockClear();
    $.ajax._mockAjax._successCallback = undefined;

    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => {
          store[k] = String(v);
        },
        removeItem: (k) => {
          delete store[k];
        },
        clear: () => {
          store = {};
        },
      };
    })();

    global.webgoat = {
      config: {
        jwtDemoPassword: 'config-password',
      },
      customjs: {},
    };

    // Load the module under test (executes top-level code including login call)
    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  test('login uses non-hardcoded, configurable password and never literal secret', () => {
    // Arrange
    const ajaxCall = $.ajax._mockAjax;
    expect(ajaxCall).toHaveBeenCalledTimes(1);

    const firstCallArgs = ajaxCall.mock.calls[0][0];
    const body = JSON.parse(firstCallArgs.data);

    // Assert: password is derived from configuration, not hard-coded literal
    expect(body.password).toBe('config-password');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('success callback stores tokens only when present and as strings', () => {
    // Arrange
    const ajaxCall = $.ajax._mockAjax;
    const successCb = ajaxCall._successCallback;

    // Act
    successCb({ access_token: 'access123', refresh_token: 'refresh123' });

    // Assert
    expect(global.localStorage.getItem('access_token')).toBe('access123');
    expect(global.localStorage.getItem('refresh_token')).toBe('refresh123');

    // Act again with missing tokens
    global.localStorage.clear();
    successCb({});

    // Assert - values should remain null because no tokens provided
    expect(global.localStorage.getItem('access_token')).toBeNull();
    expect(global.localStorage.getItem('refresh_token')).toBeNull();
  });

  test('newToken uses server-returned tokens (no undefined apiToken/refreshToken)', () => {
    // Arrange
    global.localStorage.setItem('access_token', 'existing-access');
    global.localStorage.setItem('refresh_token', 'existing-refresh');

    const ajaxCall = $.ajax._mockAjax;
    ajaxCall.mockClear();
    ajaxCall._successCallback = undefined;

    const module = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    module.newToken();

    // Simulate server response
    const secondCallArgs = ajaxCall.mock.calls[0][0];
    const successCb = ajaxCall._successCallback;
    const payload = JSON.parse(secondCallArgs.data);

    expect(payload.refreshToken).toBe('existing-refresh');

    successCb({ access_token: 'new-access', refresh_token: 'new-refresh' });

    // Assert
    expect(global.localStorage.getItem('access_token')).toBe('new-access');
    expect(global.localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
