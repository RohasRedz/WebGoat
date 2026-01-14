/**
 * Delta tests for jwt-refresh.js focusing on removal of the hard-coded password.
 * These tests verify that:
 *  - The login function no longer embeds a literal password in the request body.
 *  - The password is passed as a parameter and can come from configuration.
 */

const $ = require('jquery');
global.$ = $;
global.jQuery = $;

require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // Adjust relative path based on actual project layout if needed.

describe('jwt-refresh login (delta tests)', () => {
  beforeEach(() => {
    // Reset jQuery AJAX mock
    jest.spyOn($, 'ajax').mockImplementation(() => ({
      success: function (cb) {
        cb({ access_token: 'token', refresh_token: 'refresh' });
        return this;
      },
    }));

    global.webgoat = {
      config: {
        demoUserPassword: 'DEMO_CONFIG_PASSWORD',
      },
      customjs: {},
    };

    global.localStorage = {
      data: {},
      setItem(key, value) {
        this.data[key] = value;
      },
      getItem(key) {
        return this.data[key];
      },
    };
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.webgoat;
    delete global.localStorage;
  });

  test('login uses provided password parameter instead of hard-coded literal', () => {
    const ajaxSpy = jest.spyOn($, 'ajax');

    // Trigger document.ready handler from jwt-refresh.js
    $(document).ready();

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];

    expect(callArgs.type).toBe('POST');
    expect(callArgs.url).toBe('JWT/refresh/login');

    const body = JSON.parse(callArgs.data);
    expect(body.user).toBe('Jerry');
    // Password should come from configuration, not from a fixed literal value
    expect(body.password).toBe('DEMO_CONFIG_PASSWORD');
  });

  test('addBearerToken still uses access_token from localStorage header', () => {
    global.localStorage.setItem('access_token', 'ACCESS123');

    const headers = global.webgoat.customjs.addBearerToken();

    expect(headers.Authorization).toBe('Bearer ACCESS123');
  });
});
