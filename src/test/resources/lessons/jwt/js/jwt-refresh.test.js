import $ from 'jquery';
import '../../../main/resources/lessons/jwt/js/jwt-refresh';

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    localStorage.clear();
    window.WEBGOAT_JWT_REFRESH_PASSWORD = 'test-secret';
    jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: (cb) =>
        cb({
          access_token: 'access-123',
          refresh_token: 'refresh-456',
        }),
    }));
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('login uses externalized password configuration and stores tokens', () => {
    const user = 'Jerry';

    // login is defined in jwt-refresh.js in the global scope
    // eslint-disable-next-line no-undef
    login(user);

    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];

    expect(ajaxConfig.url).toBe('JWT/refresh/login');
    expect(ajaxConfig.type || ajaxConfig.method).toBe('POST');
    const payload = JSON.parse(ajaxConfig.data);
    expect(payload.user).toBe(user);
    expect(payload.password).toBe('test-secret');

    expect(localStorage.getItem('access_token')).toBe('access-123');
    expect(localStorage.getItem('refresh_token')).toBe('refresh-456');
  });

  test('login throws when external password is not configured', () => {
    delete window.WEBGOAT_JWT_REFRESH_PASSWORD;

    // eslint-disable-next-line no-undef
    expect(() => login('Jerry')).toThrow(
      'JWT refresh password is not configured'
    );
  });
});
