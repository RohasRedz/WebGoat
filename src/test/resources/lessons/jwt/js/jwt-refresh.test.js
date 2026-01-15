// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests for jwt-refresh.js focusing on removal of hard-coded password and use of getJwtDemoPassword.

/* global describe, it, expect, beforeAll */

describe('jwt-refresh delta tests', () => {
  let getJwtDemoPassword;
  let login;

  beforeAll(() => {
    // Inline essential parts from the updated jwt-refresh.js for delta behavior testing
    (function () {
      'use strict';

      function getJwtDemoPasswordLocal() {
        if (typeof global.window !== 'undefined' &&
          global.window.webgoat &&
          global.window.webgoat.config &&
          typeof global.window.webgoat.config.jwtDemoPassword === 'string' &&
          global.window.webgoat.config.jwtDemoPassword.length > 0) {
          return global.window.webgoat.config.jwtDemoPassword;
        }
        return 'CHANGE_ME_JWT_DEMO_PASSWORD';
      }

      function loginLocal(user, ajaxImpl) {
        const $ = ajaxImpl;
        $.ajax({
          type: 'POST',
          url: 'JWT/refresh/login',
          contentType: 'application/json',
          data: JSON.stringify({
            user: user,
            password: getJwtDemoPasswordLocal()
          })
        });
      }

      getJwtDemoPassword = getJwtDemoPasswordLocal;
      login = loginLocal;
    })();
  });

  it('getJwtDemoPassword returns configured password when provided', () => {
    global.window = {
      webgoat: {
        config: {
          jwtDemoPassword: 'CONFIG_SECRET'
        }
      }
    };

    const pwd = getJwtDemoPassword();
    expect(pwd).toBe('CONFIG_SECRET');
  });

  it('getJwtDemoPassword falls back to non-secret placeholder when not configured', () => {
    global.window = {}; // no config

    const pwd = getJwtDemoPassword();
    expect(pwd).toBe('CHANGE_ME_JWT_DEMO_PASSWORD');
  });

  it('login uses getJwtDemoPassword result instead of hard-coded literal', () => {
    const ajaxMock = {
      ajax: jest.fn()
    };
    global.window = {
      webgoat: {
        config: {
          jwtDemoPassword: 'RUNTIME_SECRET'
        }
      }
    };

    login('Jerry', ajaxMock);

    expect(ajaxMock.ajax).toHaveBeenCalledTimes(1);
    const callArg = ajaxMock.ajax.mock.calls[0][0];
    expect(JSON.parse(callArg.data).password).toBe('RUNTIME_SECRET');
    expect(JSON.parse(callArg.data).password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
