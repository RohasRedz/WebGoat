const jwtRefreshModulePath = '../../../main/resources/lessons/jwt/js/jwt-refresh';

let ajaxMock;

jest.mock('jquery', () => {
  ajaxMock = jest.fn();
  return {
    ajax: ajaxMock,
  };
});

global.webgoat = {
  customjs: {},
};

beforeEach(() => {
  const store = {};
  global.localStorage = {
    getItem: jest.fn((key) => store[key]),
    setItem: jest.fn((key, value) => {
      store[key] = String(value);
    }),
  };
  ajaxMock && ajaxMock.mockReset();
  jest.resetModules();
});

describe('jwt-refresh.js (delta tests)', () => {
  test('login should not send a hard-coded password in the request body', () => {
    require(jwtRefreshModulePath);

    login('Jerry');

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const config = ajaxMock.mock.calls[0][0];

    expect(config.type).toBe('POST');
    expect(config.url).toBe('JWT/refresh/login');
    expect(config.contentType).toBe('application/json');

    const body = JSON.parse(config.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('');
  });

  test('newToken should send refresh token from localStorage and update tokens from response', () => {
    localStorage.setItem('access_token', 'oldAccess');
    localStorage.setItem('refresh_token', 'oldRefresh');

    require(jwtRefreshModulePath);

    ajaxMock.mockImplementation((cfg) => {
      const resp = { access_token: 'newAccess', refresh_token: 'newRefresh' };
      if (typeof cfg.success === 'function') {
        cfg.success(resp);
      }
      if (typeof cfg.complete === 'function') {
        cfg.complete(resp);
      }
      return {
        success: (cb) => {
          cb(resp);
          return this;
        },
      };
    });

    newToken();

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const cfg = ajaxMock.mock.calls[0][0];

    expect(cfg.type).toBe('POST');
    expect(cfg.url).toBe('JWT/refresh/newToken');
    expect(cfg.headers.Authorization).toBe('Bearer oldAccess');

    const body = JSON.parse(cfg.data);
    expect(body.refreshToken).toBe('oldRefresh');

    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'newAccess');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'newRefresh');
  });
});
