// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and correct request structure using a runtime-provided password.

const $ = require('jquery');

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      cb({ access_token: 'access', refresh_token: 'refresh' });
      return this;
    }
  }));
  return Object.assign(function () {}, {
    ajax: ajaxMock
  });
});

describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    // Reset storage between tests
    localStorage.clear();
    sessionStorage.clear();
    jest.clearAllMocks();
  });

  test('login uses password from sessionStorage and not a hard-coded value', () => {
    // Arrange: set a runtime password into sessionStorage
    const runtimePassword = 'runtime-secret';
    sessionStorage.setItem('jwt_refresh_password', runtimePassword);

    // Load the module under test (this will define login)
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act: invoke login via the global function defined by the script
    global.login('Jerry');

    // Assert: jQuery.ajax should be called with data containing the runtime password
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];
    const sentData = JSON.parse(callArgs.data);

    expect(sentData.user).toBe('Jerry');
    expect(sentData.password).toBe(runtimePassword);
    // Ensure no obvious hard-coded secret is used
    expect(sentData.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to empty password when sessionStorage value is missing', () => {
    // Arrange: do not set any password in sessionStorage

    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    global.login('Jerry');

    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];
    const sentData = JSON.parse(callArgs.data);

    expect(sentData.user).toBe('Jerry');
    expect(sentData.password).toBe('');
  });
});
