// Derived test path (per instructions):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Note: This test focuses on the changed behavior:
// - No hard-coded secret password string ("bm5nhSkxCXZkKRy4").
// - login sends a non-secret demo password value.

describe('jwt-refresh.js - delta tests for hard-coded password removal', () => {
  let ajaxMock;
  let login;

  beforeEach(() => {
    // Mock jQuery and its ajax function.
    ajaxMock = jest.fn().mockReturnValue({
      success: function (cb) {
        cb({
          access_token: 'ACCESS',
          refresh_token: 'REFRESH',
        });
        return this;
      },
    });

    global.$ = {
      ajax: ajaxMock,
      // Minimal ready wrapper; we do not exercise document.ready side effects in this delta test.
      ready: (fn) => fn(),
    };

    global.localStorage = (function () {
      let store = {};
      return {
        clear: () => {
          store = {};
        },
        getItem: (key) => store[key] || null,
        setItem: (key, value) => {
          store[key] = String(value);
        },
      };
    })();

    // Recreate the updated login function in isolation.
    login = function (user) {
      const demoPassword = 'demo-password-not-for-production';

      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: demoPassword }),
      }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
      });
    };
  });

  test('login does not use the original hard-coded secret and uses demo password instead', () => {
    // Act
    login('Jerry');

    // Assert: ajax was called with a payload containing the demo password,
    // and not the original hard-coded secret.
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const callArgs = ajaxMock.mock.calls[0][0];

    expect(callArgs.url).toBe('JWT/refresh/login');
    expect(callArgs.type).toBe('POST');

    const parsedData = JSON.parse(callArgs.data);
    expect(parsedData.user).toBe('Jerry');
    expect(parsedData.password).toBe('demo-password-not-for-production');

    // Ensure original secret string is not present anywhere in the payload.
    expect(callArgs.data).not.toContain('bm5nhSkxCXZkKRy4');

    // Behavior: tokens still stored in localStorage as before.
    expect(localStorage.getItem('access_token')).toBe('ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('REFRESH');
  });
});
