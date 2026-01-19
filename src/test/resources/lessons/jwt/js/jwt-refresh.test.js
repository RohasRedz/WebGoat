// Delta tests for jwt-refresh.js focusing on the fixed hard-coded password vulnerability.
// We verify that the AJAX payload no longer contains the original secret value and instead
// uses a non-sensitive placeholder, while preserving the rest of the behavior.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Simulate a successful response with tokens to exercise the success callback.
      cb({ access_token: 'access123', refresh_token: 'refresh123' });
      return this;
    }
  }));
  return {
    ajax: ajaxMock,
    fn: {},
    ready: jest.fn()
  };
});

const $ = require('jquery');

describe('jwt-refresh login payload (delta tests)', () => {
  beforeEach(() => {
    // Reset mock call history and localStorage before each test.
    $.ajax.mockClear();
    global.localStorage = {
      store: {},
      setItem(key, value) {
        this.store[key] = value;
      },
      getItem(key) {
        return this.store[key];
      }
    };
  });

  test('login sends non-secret placeholder password instead of original hard-coded secret', () => {
    // Arrange
    // Load the updated script; this will define login() and attach handlers.
    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // The DOM-ready handler triggers an initial login('Jerry'); we use that call.
    const lastCall = $.ajax.mock.calls[$.ajax.mock.calls.length - 1];

    // Assert
    expect(lastCall).toBeDefined();
    const ajaxConfig = lastCall[0];
    const payload = JSON.parse(ajaxConfig.data);

    // Before the fix, password was "bm5nhSkxCXZkKRy4".
    // After the fix, it must no longer be that exact secret.
    expect(payload.user).toBe('Jerry');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
    expect(payload.password).toBe('dummy-password');
  });

  test('successful login still stores access and refresh tokens in localStorage', () => {
    // Arrange
    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // The ajax mock's success handler already sets tokens; ensure they are stored.
    expect(global.localStorage.getItem('access_token')).toBe('access123');
    expect(global.localStorage.getItem('refresh_token')).toBe('refresh123');
  });
});
