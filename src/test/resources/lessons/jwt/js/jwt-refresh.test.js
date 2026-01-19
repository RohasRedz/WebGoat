jest.mock('jquery', () => ({
  ajax: jest.fn(() => ({
    success: function (cb) {
      cb({ access_token: 'at', refresh_token: 'rt' });
      return this;
    }
  }))
}));

require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh login flow (delta test)', () => {
  test('login() should post user and demo-only password without hard-coded secret', () => {
    const $ = require('jquery');
    const calls = $.ajax.mock.calls;

    expect($.ajax).toHaveBeenCalled();

    const lastCallArgs = calls[calls.length - 1][0];
    expect(lastCallArgs.type).toBe('POST');
    expect(lastCallArgs.url).toBe('JWT/refresh/login');

    const body = JSON.parse(lastCallArgs.data);
    expect(body.user).toBe('Jerry');

    expect(body.password).toBeDefined();
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
