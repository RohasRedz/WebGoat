// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on the removal of a hard-coded password.
 *
 * The updated implementation:
 * - No longer embeds a literal password in the source.
 * - Obtains the password at runtime from either:
 *     - document.body.dataset.jwtDemoPassword, or
 *     - an input element with id="jwt-demo-password".
 * - Aborts login if no password is available.
 *
 * These tests verify:
 * - login() is not invoked when no password can be resolved.
 * - login() is invoked with a runtime-provided password when available.
 * - No hard-coded password value appears in the payload.
 */

describe('jwt-refresh delta tests', () => {
  let originalBody;
  let originalAjax;

  beforeEach(() => {
    // Preserve original DOM state and jQuery.ajax
    originalBody = document.body.innerHTML;
    originalAjax = $.ajax;
  });

  afterEach(() => {
    document.body.innerHTML = originalBody;
    $.ajax = originalAjax;
  });

  it('does not attempt login when no configured password is available', () => {
    // Arrange: clear body and ensure there is no password configuration
    document.body.innerHTML = '<div id="root"></div>';

    // Spy on login and ajax
    const loginSpy = jest.spyOn(window, 'login');
    $.ajax = jest.fn();

    // Act: re-run document ready handler from the script
    // The script attaches the ready handler at load time; simulate by calling it explicitly
    $(document).ready._callbacks.forEach((cb) => cb());

    // Assert: login should not be called because getConfiguredPassword() returns null
    expect(loginSpy).not.toHaveBeenCalled();
    expect($.ajax).not.toHaveBeenCalled();
  });

  it('uses runtime-provided password from body data attribute and calls login once', () => {
    // Arrange: configure password via data attribute
    document.body.innerHTML = '<div id="root"></div>';
    document.body.dataset.jwtDemoPassword = 'runtime-secret';

    const ajaxMock = jest.fn().mockReturnValue({
      success: function (cb) {
        cb({ access_token: 'at', refresh_token: 'rt' });
        return this;
      }
    });
    $.ajax = ajaxMock;

    // Act: invoke login flow directly using configured password
    const user = 'Jerry';
    const password = window.getConfiguredPassword();
    window.login(user, password);

    // Assert
    expect(password).toBe('runtime-secret');
    expect(ajaxMock).toHaveBeenCalledTimes(1);

    const arg = ajaxMock.mock.calls[0][0];
    expect(arg.url).toBe('JWT/refresh/login');
    expect(arg.type).toBe('POST');

    const payload = JSON.parse(arg.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('runtime-secret');
  });

  it('uses password from input element when body data attribute is not set', () => {
    // Arrange: no data attribute, but an input with id="jwt-demo-password"
    document.body.innerHTML =
      '<input id="jwt-demo-password" value="input-secret" />';

    const ajaxMock = jest.fn().mockReturnValue({
      success: function (cb) {
        cb({ access_token: 'at', refresh_token: 'rt' });
        return this;
      }
    });
    $.ajax = ajaxMock;

    // Act
    const user = 'Jerry';
    const password = window.getConfiguredPassword();
    window.login(user, password);

    // Assert
    expect(password).toBe('input-secret');
    expect(ajaxMock).toHaveBeenCalledTimes(1);

    const arg = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(arg.data);
    expect(payload.password).toBe('input-secret');
  });

  it('aborts login and logs warning when password is empty', () => {
    // Arrange
    const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    const ajaxMock = jest.fn();
    $.ajax = ajaxMock;

    // Act
    window.login('Jerry', '');

    // Assert
    expect(consoleWarnSpy).toHaveBeenCalled();
    expect(ajaxMock).not.toHaveBeenCalled();

    consoleWarnSpy.mockRestore();
  });
});
