// Derived test file path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Unit tests for jwt-refresh.js focusing on the removal of hardcoded password
 * and usage of getSafePassword() to source the credential externally.
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      cb({ access_token: 'atk', refresh_token: 'rtk' });
    },
  }));
  return {
    ajax: ajaxMock,
  };
});

const $ = require('jquery');

describe('jwt-refresh – externalized password handling', () => {
  beforeEach(() => {
    jest.resetModules();
    global.localStorage = {
      store: {},
      setItem(key, value) {
        this.store[key] = value;
      },
      getItem(key) {
        return this.store[key];
      },
    };
  });

  test('login should not use hardcoded password and must invoke getSafePassword', () => {
    // Arrange
    const webgoat = {
      config: {
        getJwtRefreshPassword: jest.fn(() => 'external-secret'),
      },
      customjs: {},
    };
    global.webgoat = webgoat;

    // Act: require after setting global so that getSafePassword sees config
    jest.isolateModules(() => {
      require('./jwt-refresh.js');
    });

    // Assert: ensure our external provider was used
    expect(webgoat.config.getJwtRefreshPassword).toHaveBeenCalled();

    // Additionally, ensure ajax payload does not contain the old hardcoded password literal
    const ajaxCall = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxCall.data);
    expect(payload.password).toBe('external-secret');
    expect(Object.values(payload).join('')).not.toContain('bm5nhSkxCXZkKRy4');
  });
});
