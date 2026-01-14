/**
 * Delta tests for jwt-refresh.js focusing on the hard-coded password removal:
 * - verifies that login() no longer uses a literal password
 * - verifies that getJwtLoginPassword() pulls the value from a meta tag and falls back safely
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      cb({ access_token: 'at', refresh_token: 'rt' });
      return { success: jest.fn() };
    },
  }));
  return {
    ajax: ajaxMock,
  };
});

const $ = require('jquery');

describe('jwt-refresh password handling (delta tests)', () => {
  let originalDocument;

  beforeEach(() => {
    originalDocument = global.document;

    const metaTag = {
      getAttribute: jest.fn().mockReturnValue('metaSecret'),
    };
    const querySelector = jest.fn().mockReturnValue(metaTag);

    global.document = {
      querySelector,
    };

    global.localStorage = {
      store: {},
      setItem(key, value) {
        this.store[key] = value;
      },
      getItem(key) {
        return this.store[key];
      },
    };

    jest.resetModules();
  });

  afterEach(() => {
    global.document = originalDocument;
  });

  test('login uses password from getJwtLoginPassword (meta tag) instead of hard-coded literal', () => {
    const jwtModule = require('../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    jest.spyOn(global.document, 'querySelector').mockReturnValue({
      getAttribute: jest.fn().mockReturnValue('metaSecret'),
    });

    const ajaxSpy = jest.spyOn($, 'ajax');

    jwtModule.login('Jerry');

    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxSpy.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('metaSecret');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('getJwtLoginPassword falls back to empty string when no meta tag is present', () => {
    jest.resetModules();
    global.document.querySelector = jest.fn().mockReturnValue(null);
    const jwtModule = require('../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    const password = jwtModule.getJwtLoginPassword();

    expect(password).toBe('');
  });
});
