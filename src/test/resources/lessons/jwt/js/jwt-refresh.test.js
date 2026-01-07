/**
 * Delta tests for jwt-refresh.js
 */

describe('jwt-refresh.js delta tests', () => {
  let originalAjax;
  let originalWindowPassword;
  let ajaxCalls;

  beforeEach(() => {
    ajaxCalls = [];
    originalAjax = global.$ && global.$.ajax;
    global.$ = global.$ || {};
    global.$.ajax = jest.fn((options) => {
      ajaxCalls.push(options);
      return {
        success: (cb) => {
          cb({ access_token: 'access', refresh_token: 'refresh' });
        }
      };
    });

    global.localStorage = {
      store: {},
      setItem(key, value) {
        this.store[key] = value;
      },
      getItem(key) {
        return this.store[key];
      }
    };

    originalWindowPassword = global.window && global.window.WEBGOAT_JWT_DEMO_PASSWORD;
    global.window = global.window || {};
  });

  afterEach(() => {
    if (originalAjax) {
      global.$.ajax = originalAjax;
    }
    if (originalWindowPassword === undefined) {
      delete global.window.WEBGOAT_JWT_DEMO_PASSWORD;
    } else {
      global.window.WEBGOAT_JWT_DEMO_PASSWORD = originalWindowPassword;
    }
  });

  it('login uses window.WEBGOAT_JWT_DEMO_PASSWORD when present', () => {
    global.window.WEBGOAT_JWT_DEMO_PASSWORD = 'OVERRIDDEN_SECURE_VALUE';

    login('Jerry');

    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const call = ajaxCalls[0];
    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('OVERRIDDEN_SECURE_VALUE');
    expect(JSON.stringify(payload)).not.toContain('bm5nhSkxCXZkKRy4');
  });

  it('login falls back to placeholder password when override is not set', () => {
    delete global.window.WEBGOAT_JWT_DEMO_PASSWORD;

    login('Jerry');

    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const call = ajaxCalls[0];
    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('OVERRIDE_ME_WITH_SECURE_CONFIG');
    expect(JSON.stringify(payload)).not.toContain('bm5nhSkxCXZkKRy4');
  });
});
