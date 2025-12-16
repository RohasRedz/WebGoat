// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests for jwt-refresh.js focusing on the removal of hard-coded password
// and use of a configurable password source.
//
// Before fix (vulnerable):
//   data: JSON.stringify({user: user, password: "bm5nhSkxCXZkKRy4"})
//
// After fix (secure):
//   var configuredPassword = (typeof JWT_REFRESH_PASSWORD !== 'undefined' && JWT_REFRESH_PASSWORD)
//       ? JWT_REFRESH_PASSWORD
//       : 'CHANGE_ME_SECURELY_CONFIGURED_PASSWORD';
//   ...
//   data: JSON.stringify({ user: user, password: configuredPassword });

/* eslint-env jest */

// TODO: adjust path if module resolution differs in the real project.
const fs = require('fs');
const path = require('path');

describe('jwt-refresh.js delta tests', () => {
  let originalAjax;

  beforeEach(() => {
    // Mock jQuery and its ajax method
    global.$ = {
      ajax: jest.fn().mockReturnValue({
        success: function (cb) {
          // Immediately invoke success callback with a fake response
          cb({
            access_token: 'access-token-from-server',
            refresh_token: 'refresh-token-from-server',
          });
          return this;
        },
      }),
    };

    // Mock localStorage
    const store = {};
    global.localStorage = {
      setItem: jest.fn((k, v) => {
        store[k] = v;
      }),
      getItem: jest.fn((k) => store[k]),
    };

    // Reset configurable password global between tests
    if (typeof global.JWT_REFRESH_PASSWORD !== 'undefined') {
      delete global.JWT_REFRESH_PASSWORD;
    }

    // Load the script under test (simulate browser execution)
    const scriptPath = path.join(
      __dirname,
      '..',
      '..',
      '..',
      'main',
      'resources',
      'lessons',
      'jwt',
      'js',
      'jwt-refresh.js'
    );
    const code = fs.readFileSync(scriptPath, 'utf8');
    // Execute in current context
    // eslint-disable-next-line no-eval
    eval(code);
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('login() should send configured password from JWT_REFRESH_PASSWORD (no hard-coded secret)', () => {
    // Arrange
    global.JWT_REFRESH_PASSWORD = 'CONFIGURED_SECRET';
    // Re-evaluate the script to pick up the new global
    // eslint-disable-next-line no-eval
    eval(
      fs.readFileSync(
        path.join(
          __dirname,
          '..',
          '..',
          '..',
          'main',
          'resources',
          'lessons',
          'jwt',
          'js',
          'jwt-refresh.js'
        ),
        'utf8'
      )
    );

    // Act
    // login is defined globally by the script
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('CONFIGURED_SECRET');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4'); // old hard-coded value must not be used
  });

  test('login() should fall back to non-secret placeholder when JWT_REFRESH_PASSWORD is undefined', () => {
    // Act
    global.login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];
    const body = JSON.parse(ajaxConfig.data);

    expect(body.user).toBe('Jerry');
    // Placeholder must be used when no external configuration is provided
    expect(body.password).toBe('CHANGE_ME_SECURELY_CONFIGURED_PASSWORD');
  });
});
