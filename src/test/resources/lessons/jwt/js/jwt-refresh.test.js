const $ = require('jquery');

global.$ = $;

describe('jwt-refresh delta tests', () => {
  let ajaxSpy;

  beforeEach(() => {
    document.body.innerHTML = '';
    ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: function (cb) {
        cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
        return this;
      }
    }));
    localStorage.clear();
    jest.resetModules();

    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    ajaxSpy.mockRestore();
  });

  test('login aborts when #jwt-password field is not present (no AJAX call)', () => {
    expect(typeof global.login).toBe('function');
    global.login('Jerry');
    expect(ajaxSpy).not.toHaveBeenCalled();
  });

  test('login uses value from #jwt-password field and not a hard-coded password', () => {
    const input = document.createElement('input');
    input.id = 'jwt-password';
    input.value = 'UserSuppliedSecret';
    document.body.appendChild(input);

    global.login('Jerry');

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];

    expect(callArgs.type).toBe('POST');
    expect(callArgs.url).toBe('JWT/refresh/login');
    expect(callArgs.contentType).toBe('application/json');

    const body = JSON.parse(callArgs.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('UserSuppliedSecret');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');

    expect(localStorage.getItem('access_token')).toBe('ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('REFRESH');
  });

  test('getUserPassword enforces basic length limit (password longer than 128 chars is rejected)', () => {
    const input = document.createElement('input');
    input.id = 'jwt-password';
    input.value = 'a'.repeat(129);
    document.body.appendChild(input);

    global.login('Jerry');

    expect(ajaxSpy).not.toHaveBeenCalled();
  });
});
