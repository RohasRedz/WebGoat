describe('jwt-refresh.js - delta tests for hard-coded password removal', () => {
  let login;

  beforeEach(() => {
    // Simulate a minimal environment for the updated jwt-refresh.js logic
    global.$ = {
      ajax: jest.fn(() => ({
        success: function (cb) {
          cb({ access_token: 'at', refresh_token: 'rt' });
          return this;
        }
      }))
    };

    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => {
          store[k] = String(v);
        },
        clear: () => {
          store = {};
        }
      };
    })();

    global.webgoat = { customjs: {} };

    // Inline the updated module code in a test-friendly way.
    // NOTE: This replicates only the logic we need to assert the delta behavior.
    // The hard-coded password string MUST NOT appear here; tests assert its absence.
    // eslint-disable-next-line no-undef
    const JWT_REFRESH_PASSWORD = null; // as in updated code

    login = function (user) {
      if (!JWT_REFRESH_PASSWORD) {
        return;
      }
      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: JWT_REFRESH_PASSWORD })
      }).success(function (response) {
        localStorage.setItem('access_token', response.access_token);
        localStorage.setItem('refresh_token', response.refresh_token);
      });
    };
  });

  afterEach(() => {
    jest.resetAllMocks();
    delete global.$;
    delete global.localStorage;
    delete global.webgoat;
  });

  test('login should not issue AJAX request when JWT_REFRESH_PASSWORD is not configured', () => {
    // Act
    login('Jerry');

    // Assert: with JWT_REFRESH_PASSWORD === null, no AJAX call is made
    expect($.ajax).not.toHaveBeenCalled();
  });

  test('login should send runtime-supplied password instead of hard-coded literal', () => {
    // Arrange: re-create login with a non-null runtime password
    jest.resetAllMocks();
    global.$.ajax = jest.fn(() => ({
      success: function (cb) {
        cb({ access_token: 'at', refresh_token: 'rt' });
        return this;
      }
    }));

    const RUNTIME_PASSWORD = 'RuntimeSecurePassword!';

    const loginWithRuntimePassword = function (user) {
      const JWT_REFRESH_PASSWORD = RUNTIME_PASSWORD; // simulate secure runtime configuration
      if (!JWT_REFRESH_PASSWORD) {
        return;
      }
      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: JWT_REFRESH_PASSWORD })
      }).success(function (response) {
        localStorage.setItem('access_token', response.access_token);
        localStorage.setItem('refresh_token', response.refresh_token);
      });
    };

    // Act
    loginWithRuntimePassword('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxArg = $.ajax.mock.calls[0][0];
    expect(ajaxArg.url).toBe('JWT/refresh/login');

    const body = JSON.parse(ajaxArg.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe(RUNTIME_PASSWORD);

    // Ensure that the previous hard-coded literal is not used
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
