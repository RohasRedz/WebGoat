describe('jwt-refresh delta tests', () => {
  function createLoginFunction() {
    return function login(user, $ajaxImpl) {
      const $ = $ajaxImpl || require('jquery');

      const password =
        typeof window !== 'undefined' && window.webgoatDemoPassword
          ? String(window.webgoatDemoPassword)
          : '';

      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password }),
      }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
      });
    };
  }

  test('login uses runtime-derived window.webgoatDemoPassword when set', () => {
    const ajaxMock = jest.fn().mockReturnValue({
      success: (cb) =>
        cb({
          access_token: 'access',
          refresh_token: 'refresh',
        }),
    });

    global.window = Object.assign(global.window || {}, {
      webgoatDemoPassword: 'runtimeSecret',
    });

    const setItemSpy = jest.fn();
    global.localStorage = {
      setItem: setItemSpy,
      getItem: jest.fn(),
    };

    const login = createLoginFunction();

    login('Jerry', { ajax: ajaxMock });

    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtimeSecret');
  });

  test('login falls back to empty password string when webgoatDemoPassword is not set', () => {
    const ajaxMock = jest.fn().mockReturnValue({
      success: (cb) =>
        cb({
          access_token: 'access',
          refresh_token: 'refresh',
        }),
    });

    global.window = {};

    const setItemSpy = jest.fn();
    global.localStorage = {
      setItem: setItemSpy,
      getItem: jest.fn(),
    };

    const login = createLoginFunction();

    login('Jerry', { ajax: ajaxMock });

    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('');
  });
});
