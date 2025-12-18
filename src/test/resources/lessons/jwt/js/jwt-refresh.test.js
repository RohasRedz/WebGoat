// Batch 2 - Derived test path (assumed): src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// TODO: Adjust test path/module resolution to match the actual project structure if needed.

const $ = require('jquery');

// Jest automatically hoists jest.mock, but we need to control $.ajax behavior.
jest.mock('jquery', () => {
  const ajaxMock = jest.fn();
  return {
    ajax: ajaxMock,
    fn: {},
    // Minimal stub for document.ready used in the file; we'll ignore its behavior in tests.
    ready: jest.fn(),
  };
});

// To load the module after mocks are set
function loadModule() {
  // Clear require cache so each test gets a fresh copy
  jest.resetModules();
  return require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: Adjust relative path as per actual project
}

describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure global window/webgoat can be controlled in each test
    global.window = {};
    global.webgoat = {}; // Some code may expect global webgoat
  });

  test('login fails closed when password configuration is missing', () => {
    // Arrange
    const module = loadModule(); // eslint-disable-line no-unused-vars
    // window.webgoat.config.jwtPassword is intentionally undefined

    // Act
    // login is a global function defined in the module
    global.login('Jerry');

    // Assert
    expect($.ajax).not.toHaveBeenCalled();
  });

  test('login reads password from configuration and uses it in AJAX request', () => {
    // Arrange
    global.window.webgoat = {
      config: {
        jwtPassword: 'CONFIG_PASSWORD',
      },
    };
    const module = loadModule(); // eslint-disable-line no-unused-vars

    // Act
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];

    expect(ajaxConfig.type).toBe('POST');
    expect(ajaxConfig.url).toBe('JWT/refresh/login');
    expect(ajaxConfig.contentType).toBe('application/json');

    const body = JSON.parse(ajaxConfig.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('CONFIG_PASSWORD');

    // Ensure no hard-coded password is present in request data
    expect(ajaxConfig.data).not.toContain('bm5nhSkxCXZkKRy4');
  });
});
