/**
 * Delta tests for jwt-refresh.js focusing on removal of hardcoded password and
 * using getUserPassword() as the source for the login password field.
 */

describe('jwt-refresh login payload', () => {
  let originalAjax;
  let originalGetUserPassword;
  let ajaxCalls;

  beforeEach(() => {
    // Mock jQuery ajax
    originalAjax = global.$ && global.$.ajax;
    ajaxCalls = [];
    global.$ = global.$ || {};
    global.$.ajax = jest.fn((options) => {
      ajaxCalls.push(options);
      return { success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) };
    });

    // Provide a deterministic implementation of getUserPassword
    originalGetUserPassword = global.getUserPassword;
    global.getUserPassword = jest.fn(() => 'secure-from-hook');

    // Load the module under test after mocks are in place
    // eslint-disable-next-line global-require
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    // Restore ajax
    if (originalAjax) {
      global.$.ajax = originalAjax;
    }

    // Restore getUserPassword
    if (originalGetUserPassword !== undefined) {
      global.getUserPassword = originalGetUserPassword;
    } else {
      delete global.getUserPassword;
    }

    // Clear module cache so that require re-evaluates the script per test
    jest.resetModules();
  });

  test('login uses getUserPassword value instead of hardcoded literal', () => {
    // Arrange
    const login = global.login;
    expect(typeof login).toBe('function');

    // Act
    login('Jerry');

    // Assert
    expect(global.getUserPassword).toHaveBeenCalled();
    expect(ajaxCalls.length).toBe(1);
    const payload = JSON.parse(ajaxCalls[0].data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('secure-from-hook');
    // Ensure that the previous hardcoded value is not accidentally present
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
