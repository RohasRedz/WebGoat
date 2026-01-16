/* eslint-env jest */

const $ = require('jquery');

require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh – removal of hard-coded password', () => {
  beforeEach(() => {
    global.window = global.window || {};
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: key => store[key],
        setItem: (key, value) => {
          store[key] = value;
        },
        clear: () => {
          store = {};
        }
      };
    })();

    jest.spyOn($, 'ajax').mockImplementation(() => {
      return {
        success: function (cb) {
          cb({ access_token: 'at', refresh_token: 'rt' });
          return this;
        }
      };
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('login uses configured password instead of hard-coded literal', () => {
    const configuredPassword = 'configured-secret';
    global.window.webgoatPasswordConfig = {
      jwtRefreshPassword: configuredPassword
    };

    const ajaxSpy = jest.spyOn($, 'ajax');

    jest.resetModules();
    global.window.webgoatPasswordConfig = {
      jwtRefreshPassword: configuredPassword
    };
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    expect(ajaxSpy).toHaveBeenCalled();
    const callArgs = ajaxSpy.mock.calls[0][0];
    const payload = JSON.parse(callArgs.data);

    expect(payload.password).toBe(configuredPassword);
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to non-secret placeholder when no config is provided', () => {
    delete global.window.webgoatPasswordConfig;

    const ajaxSpy = jest.spyOn($, 'ajax');

    jest.resetModules();
    require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const callArgs = ajaxSpy.mock.calls[0][0];
    const payload = JSON.parse(callArgs.data);

    expect(payload.password).toBe('placeholder-password');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
