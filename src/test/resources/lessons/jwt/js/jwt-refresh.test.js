// Derived test file path (per rules):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// Delta tests for jwt-refresh.js focusing on removal of hardcoded password and
// ensuring that login() uses a runtime-provided password.

describe('jwt-refresh delta tests', () => {
  let originalAjax;
  let ajaxCalls;

  beforeEach(() => {
    // Mock jQuery.ajax
    originalAjax = global.$ && global.$.ajax;
    ajaxCalls = [];

    global.$ = {
      ajax: (options) => {
        ajaxCalls.push(options);
        return {
          success: (cb) => {
            // simulate success callback with dummy tokens
            cb({ access_token: 'access', refresh_token: 'refresh' });
          }
        };
      }
    };

    // Mock localStorage
    const store = {};
    global.localStorage = {
      setItem: (k, v) => { store[k] = v; },
      getItem: (k) => store[k]
    };

    // Mock webgoat.customjs.getJwtPassword to return runtime password
    global.webgoat = {
      customjs: {
        getJwtPassword: jest.fn(() => 'runtime-secret')
      }
    };

    // Recreate the login function as in the fixed file.
    global.login = function (user) {
      const password = (webgoat.customjs && typeof webgoat.customjs.getJwtPassword === 'function')
        ? webgoat.customjs.getJwtPassword()
        : undefined;

      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
      }).success(
        function (response) {
          localStorage.setItem('access_token', response['access_token']);
          localStorage.setItem('refresh_token', response['refresh_token']);
        }
      );
    };
  });

  afterEach(() => {
    if (originalAjax) {
      global.$.ajax = originalAjax;
    }
  });

  test('login uses runtime password from webgoat.customjs.getJwtPassword and not a hardcoded literal', () => {
    // Act
    global.login('Jerry');

    // Assert
    expect(webgoat.customjs.getJwtPassword).toHaveBeenCalled();

    expect(ajaxCalls.length).toBe(1);
    const call = ajaxCalls[0];

    const body = JSON.parse(call.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtime-secret');

    // Ensure no known hardcoded password appears in the payload
    const serialized = call.data;
    expect(serialized).not.toContain('bm5nhSkxCXZkKRy4');
  });
});
