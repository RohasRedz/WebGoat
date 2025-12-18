// Delta tests for jwt-refresh.js hard-coded password removal.
// NOTE: Adjust the import path if your test runner uses a different base; this assumes
// Jest is configured to load the original script in the test environment.
// TODO: If module system differs (AMD/global), adapt the require/import accordingly.

describe('jwt-refresh.js delta tests - hard-coded password removal', () => {
  let originalWebGoat;
  let originalWindowPassword;

  beforeEach(() => {
    // Preserve any existing globals we might touch
    originalWebGoat = global.webgoat;
    originalWindowPassword = global.WEBGOAT_JWT_PASSWORD;

    global.webgoat = { customjs: {} };
    global.WEBGOAT_JWT_PASSWORD = undefined;

    // JSDOM provides window and document for Jest by default in jsdom environment.
    // We need jQuery for the script; stub minimal jQuery with only ajax and ready.
    global.$ = {
      ajax: jest.fn()
    };
    $.ajax.mockReturnValue({ success: jest.fn().mockImplementation(cb => cb({})) });

    // Load the script under test after globals are prepared.
    // eslint-disable-next-line global-require
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    jest.resetModules();
    global.webgoat = originalWebGoat;
    global.WEBGOAT_JWT_PASSWORD = originalWindowPassword;
    delete global.$;
  });

  test('login should not send AJAX request when WEBGOAT_JWT_PASSWORD is not configured', () => {
    // Arrange
    // With WEBGOAT_JWT_PASSWORD undefined, getJwtPassword will return empty string
    // causing login() to bail out and not call $.ajax.

    // Act
    // login is defined globally by the required script
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    expect($.ajax).not.toHaveBeenCalled();
  });

  test('login should send AJAX request using configuration-driven password, not a hard-coded literal', () => {
    // Arrange
    jest.resetModules();
    global.$ = {
      ajax: jest.fn()
    };
    $.ajax.mockReturnValue({ success: jest.fn().mockImplementation(cb => cb({})) });

    global.webgoat = { customjs: {} };
    global.WEBGOAT_JWT_PASSWORD = 'CONFIG_SECRET';

    // eslint-disable-next-line global-require
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];
    expect(callArgs.type).toBe('POST');
    expect(callArgs.url).toBe('JWT/refresh/login');
    // Verify that the data contains the configured secret and not the old hard-coded value.
    const payload = JSON.parse(callArgs.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('CONFIG_SECRET');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
