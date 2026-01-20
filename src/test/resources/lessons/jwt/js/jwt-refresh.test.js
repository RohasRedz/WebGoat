/**
 * Delta tests for jwt-refresh.js focusing on:
 * - removal of hardcoded password
 * - safe token handling and refresh flow behavior
 */

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  let originalAjax;
  let originalWebgoat;

  beforeEach(() => {
    originalAjax = $.ajax;
    originalWebgoat = global.webgoat;
    global.webgoat = { customjs: {} };
  });

  afterEach(() => {
    $.ajax = originalAjax;
    global.webgoat = originalWebgoat;
  });

  test('login does not send a hardcoded password and stores tokens in memory', (done) => {
    // Arrange
    $.ajax = jest.fn(() => ({
      done: (cb) => {
        cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
        return { done: () => {} };
      }
    }));
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArgs = $.ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe(''); // no hardcoded secret

    expect(global.webgoat.tokens.access_token).toBe('ACCESS');
    expect(global.webgoat.tokens.refresh_token).toBe('REFRESH');
    done();
  });

  test('newToken uses in-memory tokens and updates them from response', (done) => {
    // Arrange
    global.webgoat.tokens = {
      access_token: 'OLD_ACCESS',
      refresh_token: 'OLD_REFRESH'
    };

    const ajaxMock = jest.fn(() => ({
      done: (cb) => {
        cb({ access_token: 'NEW_ACCESS', refresh_token: 'NEW_REFRESH' });
        return { done: () => {} };
      }
    }));
    $.ajax = ajaxMock;
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    // Act
    global.newToken();

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const options = ajaxMock.mock.calls[0][0];

    expect(options.headers.Authorization).toBe('Bearer OLD_ACCESS');
    const body = JSON.parse(options.data);
    expect(body.refreshToken).toBe('OLD_REFRESH');

    expect(global.webgoat.tokens.access_token).toBe('NEW_ACCESS');
    expect(global.webgoat.tokens.refresh_token).toBe('NEW_REFRESH');
    done();
  });
});
