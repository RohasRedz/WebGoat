// Test file path derived from:
// src/main/resources/lessons/jwt/js/jwt-refresh.js
// -> src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js to verify:
 * - login() no longer uses a hard-coded password literal.
 * - Password is read dynamically from configuration when available.
 */

const fs = require('fs');
const path = require('path');
const vm = require('vm');

function loadJwtRefreshInSandbox(extraGlobals = {}) {
  const filePath = path.join(
    __dirname,
    '../../../../../main/resources/lessons/jwt/js/jwt-refresh.js'
  );
  const code = fs.readFileSync(filePath, 'utf8');

  const sandbox = {
    window: {},
    document: {},
    localStorage: {
      _store: {},
      setItem(key, value) {
        this._store[key] = value;
      },
      getItem(key) {
        return this._store[key];
      },
    },
    $: {
      ajax: jest.fn().mockReturnValue({
        success: function () {
          return this;
        },
      }),
    },
    webgoat: {
      customjs: {},
      config: {
        getJwtDemoPassword: jest.fn().mockReturnValue('dynamic-password'),
      },
    },
    console,
    ...extraGlobals,
  };

  vm.createContext(sandbox);
  vm.runInContext(code, sandbox, { filename: filePath });

  return sandbox;
}

describe('jwt-refresh login behavior (delta tests)', () => {
  test('login uses dynamic password from configuration (no hard-coded secret)', () => {
    const sandbox = loadJwtRefreshInSandbox();
    const $ajaxMock = sandbox.$.ajax;

    // Call login explicitly to avoid relying on document.ready timing.
    sandbox.login('Jerry');

    expect($ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = $ajaxMock.mock.calls[0][0];

    expect(ajaxConfig.type).toBe('POST');
    expect(ajaxConfig.url).toBe('JWT/refresh/login');

    const payload = JSON.parse(ajaxConfig.data);
    expect(payload.user).toBe('Jerry');
    // Ensure the value used is the dynamic one from configuration
    expect(payload.password).toBe('dynamic-password');

    // Ensure we did in fact call into the configuration hook
    expect(sandbox.webgoat.config.getJwtDemoPassword).toHaveBeenCalled();
  });

  test('login does not contain the original hard-coded password literal in source', () => {
    const filePath = path.join(
      __dirname,
      '../../../../../main/resources/lessons/jwt/js/jwt-refresh.js'
    );
    const code = fs.readFileSync(filePath, 'utf8');

    // The old hard-coded value must not appear in the updated source
    expect(code).not.toContain('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to empty password if configuration function is missing', () => {
    const sandbox = loadJwtRefreshInSandbox({
      webgoat: {
        customjs: {},
        config: {},
      },
    });
    const $ajaxMock = sandbox.$.ajax;

    sandbox.login('Jerry');

    const ajaxConfig = $ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(ajaxConfig.data);

    expect(payload.user).toBe('Jerry');
    // Without getJwtDemoPassword(), the fallback should be an empty string,
    // not a new hard-coded secret.
    expect(payload.password).toBe('');
  });
});
