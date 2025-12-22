// src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// TODO: Adjust module loading (e.g., require/import) to match the real project bundler/test setup.

describe('jwt-refresh.js delta tests (hard-coded password removal)', () => {
  let originalAjax;
  let originalWebgoat;
  let originalWindow;

  beforeEach(() => {
    // Mock jQuery ajax
    originalAjax = global.$ && global.$.ajax;
    global.$ = {
      ajax: jest.fn().mockReturnValue({
        success: function (cb) {
          // For these tests we don't need to execute the success callback
          return this;
        },
      }),
    };

    // Mock webgoat namespace used in the file
    originalWebgoat = global.webgoat;
    global.webgoat = { customjs: {} };

    // Preserve and mock window for WEBGOAT_JWT_PASSWORD access
    originalWindow = global.window;
    global.window = global.window || {};
  });

  afterEach(() => {
    if (originalAjax) {
      global.$.ajax = originalAjax;
    } else {
      delete global.$;
    }
    global.webgoat = originalWebgoat;
    global.window = originalWindow;
    jest.resetModules();
  });

  function loadModule() {
    // Require the module under test; path may need to be adjusted in real project
    require('../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust path according to test runner
  }

  test('login uses the provided password parameter in the AJAX payload', () => {
    // Arrange
    loadModule(); // defines global login function

    const expectedUser = 'Alice';
    const expectedPassword = 'runtime-secret';

    // Act
    global.login(expectedUser, expectedPassword);

    // Assert
    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const callArgs = global.$.ajax.mock.calls[0][0];
    const payload = JSON.parse(callArgs.data);

    expect(callArgs.url).toBe('JWT/refresh/login');
    expect(payload.user).toBe(expectedUser);
    expect(payload.password).toBe(expectedPassword);
  });

  test('document.ready uses window.WEBGOAT_JWT_PASSWORD when provided', () => {
    // Arrange
    global.window.WEBGOAT_JWT_PASSWORD = 'CONFIGURED_SECRET';

    // Jest does not auto-fire document.ready for us; simulate by executing the module,
    // which binds a jQuery ready callback that runs immediately in this simplified test.
    // We simulate jQuery ready as (fn) => fn().
    const readyMock = jest.fn((fn) => fn());
    global.$.ready = readyMock;
    global.$ = Object.assign(function () {}, global.$); // allow $(...).ready in module
    global.$.fn = { ready: readyMock };

    // Re-mock ajax to capture the call from ready/login
    global.$.ajax = jest.fn().mockReturnValue({
      success: function (cb) {
        return this;
      },
    });

    // Act
    loadModule();

    // Assert: ajax was called with password taken from window.WEBGOAT_JWT_PASSWORD
    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const payload = JSON.parse(global.$.ajax.mock.calls[0][0].data);
    expect(payload.password).toBe('CONFIGURED_SECRET');
  });

  test('document.ready falls back to placeholder password when WEBGOAT_JWT_PASSWORD is missing', () => {
    // Arrange
    delete global.window.WEBGOAT_JWT_PASSWORD;

    const readyMock = jest.fn((fn) => fn());
    global.$.ready = readyMock;
    global.$ = Object.assign(function () {}, global.$);
    global.$.fn = { ready: readyMock };

    global.$.ajax = jest.fn().mockReturnValue({
      success: function (cb) {
        return this;
      },
    });

    // Act
    loadModule();

    // Assert: placeholder password should be used, not a real hard-coded secret
    const payload = JSON.parse(global.$.ajax.mock.calls[0][0].data);
    expect(payload.password).toBe('PLACEHOLDER_PASSWORD');
    // Ensure the old hard-coded value is not present
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
