const { JSDOM } = require('jsdom');

describe('jwt-refresh delta tests (externalized password & token refresh)', () => {
  let window;
  let document;

  beforeEach(() => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost',
      runScripts: 'outside-only',
    });
    window = dom.window;
    document = window.document;

    global.window = window;
    global.document = document;
    global.localStorage = (() => {
      const store = {};
      return {
        getItem: (k) => store[k] || null,
        setItem: (k, v) => { store[k] = String(v); },
        clear: () => { Object.keys(store).forEach((k) => delete store[k]); },
      };
    })();

    global.$ = {
      ajax: jest.fn().mockReturnValue({
        success: function (cb) {
          this._successCb = cb;
          return this;
        },
        triggerSuccess: function (response) {
          if (this._successCb) this._successCb(response);
        },
      }),
    };

    global.webgoat = {
      customjs: {},
    };

    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  test('login uses external password provider and does not send hard-coded password', () => {
    const expectedPassword = 'secure-from-config';

    webgoat.customjs.getJwtRefreshPassword = jest.fn().mockReturnValue(expectedPassword);

    $.ajax.mockClear();

    global.login('Jerry');

    expect(webgoat.customjs.getJwtRefreshPassword).toHaveBeenCalledTimes(1);

    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe(expectedPassword);
  });

  test('login aborts and logs error when password provider returns falsy value', () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    webgoat.customjs.getJwtRefreshPassword = jest.fn().mockReturnValue(null);

    $.ajax.mockClear();

    global.login('Jerry');

    expect($.ajax).not.toHaveBeenCalled();
    expect(consoleErrorSpy).toHaveBeenCalled();

    consoleErrorSpy.mockRestore();
  });

  test('newToken updates access_token and refresh_token from server response', () => {
    localStorage.setItem('access_token', 'old-access');
    localStorage.setItem('refresh_token', 'old-refresh');

    $.ajax.mockClear();

    global.newToken();

    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxCall = $.ajax.mock.results[0].value;
    const response = { access_token: 'new-access', refresh_token: 'new-refresh' };
    ajaxCall.triggerSuccess(response);

    expect(localStorage.getItem('access_token')).toBe('new-access');
    expect(localStorage.getItem('refresh_token')).toBe('new-refresh');
  });
});
