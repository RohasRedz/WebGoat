jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      this._successCallback = cb;
      return this;
    }
  }));
  const jq = (...args) => jq.fn.init(...args);
  jq.ajax = ajaxMock;
  jq.fn = jq.prototype = {
    init: function () {}
  };
  return jq;
});

describe('jwt-refresh delta tests', () => {
  let $;
  let webgoat;

  beforeEach(() => {
    jest.resetModules();
    $ = require('jquery');

    const storage = {};
    global.localStorage = {
      getItem: jest.fn((k) => storage[k]),
      setItem: jest.fn((k, v) => { storage[k] = v; }),
      removeItem: jest.fn((k) => { delete storage[k]; }),
      clear: jest.fn(() => { Object.keys(storage).forEach(k => delete storage[k]); })
    };

    webgoat = { customjs: {} };
    global.webgoat = webgoat;

    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.localStorage;
    delete global.webgoat;
  });

  test('login() sends non-hard-coded placeholder password and stores returned tokens', () => {
    const ajaxMock = $.ajax;
    expect(ajaxMock).toHaveBeenCalled();

    const ajaxCall = ajaxMock.mock.calls[0][0];
    const sentBody = JSON.parse(ajaxCall.data);

    expect(sentBody.user).toBe('Jerry');
    expect(sentBody.password).toBe('placeholder-password');
    expect(sentBody.password).not.toBe('bm5nhSkxCXZkKRy4');

    const response = {
      access_token: 'ACCESS123',
      refresh_token: 'REFRESH456'
    };
    const successCb = ajaxMock.mock.results[0].value._successCallback;
    successCb(response);

    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'ACCESS123');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'REFRESH456');
  });

  test('newToken() uses addBearerToken helper and updates tokens from response', () => {
    const storage = {};
    global.localStorage = {
      getItem: jest.fn((k) => storage[k]),
      setItem: jest.fn((k, v) => { storage[k] = v; }),
      removeItem: jest.fn((k) => { delete storage[k]; }),
      clear: jest.fn(() => { Object.keys(storage).forEach(k => delete storage[k]); })
    };

    localStorage.setItem('access_token', 'OLD_ACCESS');
    localStorage.setItem('refresh_token', 'OLD_REFRESH');

    const ajaxMock = $.ajax;
    ajaxMock.mockClear();

    webgoat.customjs.addBearerToken = jest.fn(() => ({
      Authorization: 'Bearer ' + localStorage.getItem('access_token')
    }));

    const jwtRefreshModule = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const newTokenFn = global.newToken || jwtRefreshModule.newToken || undefined;
    expect(typeof newTokenFn).toBe('function');

    newTokenFn();

    expect(webgoat.customjs.addBearerToken).toHaveBeenCalled();

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const callConfig = ajaxMock.mock.calls[0][0];
    expect(callConfig.headers.Authorization).toBe('Bearer OLD_ACCESS');

    const body = JSON.parse(callConfig.data);
    expect(body.refreshToken).toBe('OLD_REFRESH');

    const response = {
      access_token: 'NEW_ACCESS',
      refresh_token: 'NEW_REFRESH'
    };
    const successCb = ajaxMock.mock.results[0].value._successCallback;
    successCb(response);

    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'NEW_ACCESS');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'NEW_REFRESH');
  });
});
