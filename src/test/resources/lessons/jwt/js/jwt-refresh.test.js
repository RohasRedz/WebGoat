// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests for jwt-refresh.js focusing on changed behavior:
// - Password must no longer be hard-coded; it should be read from window.webgoatConfig.jwtPassword.

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    global.window = global.window || {};
    global.webgoatConfig = { jwtPassword: 'configSecret' };
    global.$ = jest.fn(() => ({}));
    // jQuery.ajax mock
    global.$.ajax = jest.fn(() => ({
      success: function (cb) {
        cb({ access_token: 'at', refresh_token: 'rt' });
        return this;
      },
    }));

    // localStorage mock
    const store = {};
    global.localStorage = {
      setItem: (k, v) => {
        store[k] = v;
      },
      getItem: (k) => store[k],
    };

    jest.resetModules();
  });

  test('login should use password from configuration instead of hard-coded value', () => {
    // Arrange
    const configPassword = 'configSecret';
    global.webgoatConfig.jwtPassword = configPassword;

    // Require the updated script after globals are set
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    // login is defined in jwt-refresh.js global scope
    global.login('Jerry');

    // Assert
    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const ajaxArg = global.$.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxArg.data);
    expect(payload.password).toBe(configPassword);
  });
});
