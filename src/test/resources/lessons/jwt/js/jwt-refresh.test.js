// Resolved test file path (per instructions):
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on the removal of a hard-coded password:
 * - Verifies that login(user, password) sends the provided password value in the
 *   AJAX payload, not a compiled-in literal.
 */

const $ = require('jquery');

// Load the script so that it attaches login(...) to the global scope.
beforeAll(() => {
  const fs = require('fs');
  const path = require('path');
  const vm = require('vm');

  const scriptPath = path.resolve(
    __dirname,
    '../../../../main/resources/lessons/jwt/js/jwt-refresh.js'
  );
  const code = fs.readFileSync(scriptPath, 'utf8');

  const sandbox = {
    $, 
    webgoat: { customjs: {} },
    localStorage: {
      storage: {},
      setItem(key, value) {
        this.storage[key] = value;
      },
      getItem(key) {
        return this.storage[key];
      },
    },
    document: {},
    console,
  };

  vm.runInNewContext(code, sandbox, { filename: scriptPath });
  global.login = sandbox.login;
  global.webgoat = sandbox.webgoat;
  global.localStorage = sandbox.localStorage;
});

describe('jwt-refresh login payload (delta tests)', () => {
  test('login uses provided password argument in AJAX payload', () => {
    // Arrange
    const ajaxSpy = jest.spyOn($, 'ajax').mockImplementation((options) => {
      // Immediately call success callback for test purposes
      if (typeof options === 'object' && typeof options.success === 'function') {
        options.success({ access_token: 'access', refresh_token: 'refresh' });
      }
      return { success: (cb) => cb({}) };
    });

    const user = 'Jerry';
    const suppliedPassword = 'test-password-123';

    // Act
    global.login(user, suppliedPassword);

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArg = ajaxSpy.mock.calls[0][0];
    const payload = JSON.parse(callArg.data);

    expect(payload.user).toBe(user);
    expect(payload.password).toBe(suppliedPassword);

    // The fixed implementation must not revert to the old hard-coded value.
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');

    // Cleanup
    ajaxSpy.mockRestore();
  });
});
