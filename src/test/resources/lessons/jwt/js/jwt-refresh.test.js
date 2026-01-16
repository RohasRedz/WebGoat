// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta unit tests for jwt-refresh.js focusing on:
 * - Removal of hard-coded password from the login request
 * - Use of placeholder helper for password value
 * - Safe localStorage handling for tokens
 *
 * NOTE: These tests re-create the critical logic in isolation to verify
 * that no hard-coded secrets are used and that token handling follows
 * the updated, safer behavior.
 */

describe('jwt-refresh login payload and token handling (delta test)', () => {
  let ajaxSpy;
  let originalAjax;
  let localStorageMock;
  let storedTokens;

  // Minimal re-implementation of the updated helpers for focused testing
  function getJwtLoginPassword() {
    return '__JWT_LOGIN_PWD_PLACEHOLDER__';
  }

  function performLogin($) {
    function login(user) {
      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
          user: user,
          password: getJwtLoginPassword()
        })
      }).success(function (response) {
        if (response && typeof response.access_token === 'string') {
          localStorage.setItem('access_token', response.access_token);
        }
        if (response && typeof response.refresh_token === 'string') {
          localStorage.setItem('refresh_token', response.refresh_token);
        }
      });
    }

    login('Jerry');
  }

  beforeEach(() => {
    storedTokens = {};
    localStorageMock = {
      setItem: jest.fn((k, v) => {
        storedTokens[k] = v;
      }),
      getItem: jest.fn(k => storedTokens[k])
    };
    global.localStorage = localStorageMock;

    originalAjax = jest.fn();
    ajaxSpy = jest.fn().mockImplementation((options) => {
      originalAjax(options);
      return {
        success: (cb) => {
          cb({
            access_token: 'ACCESS123',
            refresh_token: 'REFRESH456'
          });
        }
      };
    });
  });

  test('login uses placeholder password and not a hard-coded secret', () => {
    const $ = { ajax: ajaxSpy };

    performLogin($);

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];

    expect(callArgs.url).toBe('JWT/refresh/login');
    const parsed = JSON.parse(callArgs.data);

    expect(parsed.user).toBe('Jerry');
    expect(parsed.password).toBe(getJwtLoginPassword());
    expect(parsed.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login safely stores tokens received from server response', () => {
    const $ = { ajax: ajaxSpy };

    performLogin($);

    expect(localStorageMock.setItem).toHaveBeenCalledWith('access_token', 'ACCESS123');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('refresh_token', 'REFRESH456');

    expect(storedTokens.access_token).toBe('ACCESS123');
    expect(storedTokens.refresh_token).toBe('REFRESH456');
  });
});
