// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
// Delta tests for src/main/resources/lessons/jwt/js/jwt-refresh.js
// Focus: removal of hard-coded password, use of getJwtLessonPassword(), and token storage behavior.

// TODO: Adjust the relative path as needed depending on your Jest/module resolution configuration.
const fs = require('fs');
const path = require('path');

describe('jwt-refresh.js delta security behavior', () => {
  const scriptPath = path.resolve(
    __dirname,
    '../../../main/resources/lessons/jwt/js/jwt-refresh.js'
  );

  let originalLocalStorage;
  let originalDocument;
  let originalWindow;
  let original$;

  beforeEach(() => {
    // Preserve globals.
    originalLocalStorage = global.localStorage;
    originalDocument = global.document;
    originalWindow = global.window;
    original$ = global.$;

    // Minimal localStorage mock.
    const store = {};
    global.localStorage = {
      getItem: jest.fn((key) => store[key]),
      setItem: jest.fn((key, value) => {
        store[key] = value;
      }),
      removeItem: jest.fn((key) => {
        delete store[key];
      }),
      clear: jest.fn(() => {
        Object.keys(store).forEach((k) => delete store[k]);
      })
    };

    // Minimal DOM + window mocks.
    global.document = {
      getElementById: jest.fn(() => null) // default: no input element found
    };
    global.window = global.window || {};
    global.window.webgoatJwtConfig = {}; // default: no defaultPassword set

    // Minimal jQuery mock for $.ajax and $(document).ready.
    const readyCallbacks = [];
    const $mock = jest.fn((arg) => {
      if (arg === document || arg === global.document) {
        return {
          ready: (cb) => {
            readyCallbacks.push(cb);
          }
        };
      }
      return {};
    });

    $mock.ajax = jest.fn(() => {
      // Return an object exposing .success(callback) chain; call callback immediately.
      return {
        success: (cb) => {
          // Simulate successful response with tokens.
          cb({
            access_token: 'ACCESS_TOKEN_VALUE',
            refresh_token: 'REFRESH_TOKEN_VALUE'
          });
        }
      };
    });

    // Expose a helper so tests can manually trigger document.ready callbacks if needed.
    $mock.__triggerDocumentReady = () => {
      readyCallbacks.forEach((cb) => cb());
    };

    global.$ = $mock;
    global.webgoat = global.webgoat || {};
    global.webgoat.customjs = global.webgoat.customjs || {};

    // Load the script under test in each test to ensure a clean environment.
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require, import/no-dynamic-require
      require(scriptPath);
    });
  });

  afterEach(() => {
    // Restore globals.
    global.localStorage = originalLocalStorage;
    global.document = originalDocument;
    global.window = originalWindow;
    global.$ = original$;
    jest.resetModules();
    jest.clearAllMocks();
  });

  test('source code must not contain the removed hard-coded password literal', () => {
    // Arrange
    const content = fs.readFileSync(scriptPath, 'utf8');

    // Assert
    // This directly verifies that the original literal secret is not present anymore.
    expect(content).not.toContain('bm5nhSkxCXZkKRy4');
  });

  test('login() must obtain password via getJwtLessonPassword() instead of using a hard-coded literal', () => {
    // Arrange
    const content = fs.readFileSync(scriptPath, 'utf8');

    // Assert (static verification of changed behavior)
    // 1) Ensure there is a helper function getJwtLessonPassword.
    expect(content).toMatch(/function\s+getJwtLessonPassword\s*\(/);

    // 2) Ensure login uses the helper to obtain the password.
    //    This regex checks that login function references getJwtLessonPassword().
    expect(content).toMatch(/login\s*\([\s\S]*?getJwtLessonPassword\s*\(\)/);

    // 3) Ensure there is no remaining inlined password literal within the JSON.stringify call.
    expect(content).not.toMatch(/password\s*:\s*["']bm5nhSkxCXZkKRy4["']/);
  });

  test('tokens must be stored in localStorage after successful AJAX login call using the derived password', () => {
    // Arrange
    // Provide a DOM input so getJwtLessonPassword() returns a non-empty value.
    const passwordInput = { value: 'NON_HARDCODED_PASSWORD' };
    global.document.getElementById = jest.fn((id) =>
      id === 'jwt-lesson-password' ? passwordInput : null
    );

    // Re-require the script so it sees the updated DOM mock.
    jest.resetModules();
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require, import/no-dynamic-require
      require(scriptPath);
    });

    // Act
    // Trigger the behavior that calls login('Jerry') via document.ready.
    global.$.__triggerDocumentReady();

    // Assert
    // 1) Verify that $.ajax was called and that the payload contains the non-hard-coded password.
    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = global.$.ajax.mock.calls[0][0];
    expect(ajaxConfig.url).toBe('JWT/refresh/login');

    const payload = JSON.parse(ajaxConfig.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('NON_HARDCODED_PASSWORD');

    // 2) Verify that access_token and refresh_token are written to localStorage.
    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'access_token',
      'ACCESS_TOKEN_VALUE'
    );
    expect(global.localStorage.setItem).toHaveBeenCalledWith(
      'refresh_token',
      'REFRESH_TOKEN_VALUE'
    );
  });
});
