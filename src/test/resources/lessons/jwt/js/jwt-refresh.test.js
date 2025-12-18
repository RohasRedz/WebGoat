const $ = require('jquery');

global.$ = $;

describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    global.window = global;
    window.WEBGOAT_JWT_PASSWORD = undefined;
    jest.spyOn($, 'ajax').mockClear();
  });

  test('login uses getJwtPassword() and WEBGOAT_JWT_PASSWORD for password', () => {
    window.WEBGOAT_JWT_PASSWORD = 'runtime-secret';
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((options) => {
      if (typeof options.success === 'function') {
        options.success({
          access_token: 'a-token',
          refresh_token: 'r-token',
        });
      }
      return { success: (fn) => fn && fn({ access_token: 'a-token', refresh_token: 'r-token' }) };
    });

    login('Jerry');

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtime-secret');
  });

  test('no hard-coded password literal is present in payload when WEBGOAT_JWT_PASSWORD is undefined', () => {
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((options) => {
      if (typeof options.success === 'function') {
        options.success({
          access_token: 'a-token',
          refresh_token: 'r-token',
        });
      }
      return { success: (fn) => fn && fn({ access_token: 'a-token', refresh_token: 'r-token' }) };
    });

    login('Jerry');

    const body = JSON.parse(ajaxSpy.mock.calls[0][0].data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
