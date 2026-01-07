jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Simulate async success callback
      cb({ access_token: 'access', refresh_token: 'refresh' });
      return this;
    }
  }));
  const $ = function () {};
  $.ajax = ajaxMock;
  $.ajaxMock = ajaxMock; // expose for assertions
  return $;
});

const $ = require('jquery');

// Load the script under test; it will attach functions to global scope.
require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh getJwtPassword and login behavior (delta tests)', () => {
  beforeEach(() => {
    // Reset global webgoat.customjs before each test
    global.webgoat = { customjs: {} };
    localStorage.clear();
    $.ajaxMock.mockClear();
  });

  test('getJwtPassword returns configured jwtPassword when defined', () => {
    global.webgoat.customjs.jwtPassword = 'runtimeSecret';

    // getJwtPassword is defined in the script global scope
    const password = global.getJwtPassword();

    expect(password).toBe('runtimeSecret');
  });

  test('getJwtPassword returns empty string when jwtPassword is not defined', () => {
    const password = global.getJwtPassword();

    expect(password).toBe('');
  });

  test('login uses getJwtPassword value in AJAX body', () => {
    global.webgoat.customjs.jwtPassword = 'runtimeSecret';

    // Spy on getJwtPassword to ensure it is used
    const spy = jest.spyOn(global, 'getJwtPassword');

    global.login('Jerry');

    expect(spy).toHaveBeenCalled();

    expect($.ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajaxMock.mock.calls[0][0];
    const dataSent = JSON.parse(ajaxConfig.data);

    expect(dataSent.user).toBe('Jerry');
    expect(dataSent.password).toBe('runtimeSecret');
  });

  test('login sends empty password when jwtPassword is not configured (no hardcoded secret)', () => {
    // Do not set webgoat.customjs.jwtPassword, so getJwtPassword returns ''
    global.login('Jerry');

    const ajaxConfig = $.ajaxMock.mock.calls[0][0];
    const dataSent = JSON.parse(ajaxConfig.data);

    expect(dataSent.user).toBe('Jerry');
    expect(dataSent.password).toBe(''); // verifies no hardcoded secret is used
  });
});
