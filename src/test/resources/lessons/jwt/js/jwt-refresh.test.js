jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      // Simulate async success with sample tokens
      cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
      return { success: jest.fn() };
    }
  }));
  const $ = function () {};
  $.ajax = ajaxMock;
  $.mockAjax = ajaxMock;
  return $;
});

const $ = require('jquery');

describe('jwt-refresh.js - delta tests for credential and token handling', () => {
  let originalWindow;
  let originalConsole;

  beforeEach(() => {
    originalWindow = global.window;
    originalConsole = global.console;
    global.window = {
      webgoatConfig: {
        jwtLoginPassword: 'secure-runtime-password'
      }
    };
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (key) => store[key],
        setItem: (key, value) => {
          store[key] = String(value);
        },
        clear: () => {
          store = {};
        }
      };
    })();
    global.console = { warn: jest.fn(), log: jest.fn(), error: jest.fn() };

    // jQuery ready stub
    $.mockReadyHandlers = [];
    $.fn = { ready: (fn) => $.mockReadyHandlers.push(fn) };
    global.$ = $;

    // Load module under test
    // TODO: Adjust relative path if project structure differs.
    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });
  });

  afterEach(() => {
    global.window = originalWindow;
    global.console = originalConsole;
    delete global.localStorage;
    delete global.$;
    jest.clearAllMocks();
    jest.resetModules();
  });

  test('getJwtLoginPassword should read password from window.webgoatConfig when present', () => {
    // Arrange
    // getJwtLoginPassword is defined in the global scope of jwt-refresh.js
    const getJwtLoginPassword = global.getJwtLoginPassword || global.window.getJwtLoginPassword;

    // Act
    const password = getJwtLoginPassword();

    // Assert
    expect(password).toBe('secure-runtime-password');
  });

  test('login should use the provided password argument in AJAX payload (no hard-coded secret)', () => {
    // Arrange
    const login = global.login || global.window.login;
    const user = 'Jerry';
    const password = 'runtime-password';

    // Act
    login(user, password);

    // Assert
    expect($.mockAjax).toHaveBeenCalledTimes(1);
    const ajaxCallArg = $.mockAjax.mock.calls[0][0];
    expect(ajaxCallArg.type).toBe('POST');
    expect(ajaxCallArg.url).toBe('JWT/refresh/login');
    const body = JSON.parse(ajaxCallArg.data);
    expect(body).toEqual({ user: 'Jerry', password: 'runtime-password' });
  });

  test('document ready should attempt login only when a password is available from configuration', () => {
    // Arrange
    // Trigger stored ready handlers to simulate DOM ready
    $.mockReadyHandlers.forEach((fn) => fn());

    // Assert
    expect($.mockAjax).toHaveBeenCalledTimes(1);
    const ajaxCallArg = $.mockAjax.mock.calls[0][0];
    const body = JSON.parse(ajaxCallArg.data);
    expect(body.password).toBe('secure-runtime-password');
  });

  test('document ready should not send login request when password is missing', () => {
    // Arrange
    // Remove jwtLoginPassword from config and reload module
    global.window.webgoatConfig = {};
    jest.resetModules();
    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // Clear previous calls
    $.mockAjax.mockClear();

    // Act
    $.mockReadyHandlers.forEach((fn) => fn());

    // Assert
    expect($.mockAjax).not.toHaveBeenCalled();
    expect(console.warn).toHaveBeenCalledWith(
      'JWT login password is not configured; login request not sent.'
    );
  });
});
