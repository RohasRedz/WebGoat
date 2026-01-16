// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// NOTE: This delta test focuses only on the changed behavior for jwt-refresh.js:
// - No hard-coded or obfuscated password must be present in the module exports.
// - Password must be obtained at runtime via getRuntimePassword().
// - login() must not be called when getRuntimePassword() returns a falsy value.

// TODO: Adjust the require path below according to the actual bundling/module system if needed.
// Here we assume a Node/Jest environment where the JS file can be required directly.
const path = require('path');
const fs = require('fs');

describe('jwt-refresh.js delta tests', () => {
  const scriptPath = path.resolve(
    __dirname,
    '../../../main/resources/lessons/jwt/js/jwt-refresh.js'
  );

  let originalWindow;
  let originalDocument;
  let originalLocalStorage;
  let $;

  beforeAll(() => {
    // Set up a minimal jsdom-like environment for the IIFE to run
    originalWindow = global.window;
    originalDocument = global.document;
    originalLocalStorage = global.localStorage;

    global.window = global;
    global.document = {
      getElementById: jest.fn(),
    };
    const storage = {};
    global.localStorage = {
      getItem: jest.fn((key) => storage[key] || null),
      setItem: jest.fn((key, value) => {
        storage[key] = String(value);
      }),
    };

    // Minimal jQuery stub with only what we need: ready and ajax().done()
    $ = function (selector) {
      if (selector === document) {
        return {
          ready: (cb) => {
            // Immediately invoke the ready callback for test purposes
            cb();
          },
        };
      }
      return {};
    };
    $.ajax = jest.fn(() => {
      return {
        done: (cb) => {
          // Simulate success with a token response for tests that care
          cb({ access_token: 'acc', refresh_token: 'ref' });
        },
      };
    });
    global.$ = $;
  });

  afterAll(() => {
    global.window = originalWindow;
    global.document = originalDocument;
    global.localStorage = originalLocalStorage;
    delete global.$;
  });

  beforeEach(() => {
    jest.resetModules();
    if (global.window) {
      global.window.webgoat = undefined;
    }
    if (global.document && typeof global.document.getElementById === 'function') {
      global.document.getElementById = jest.fn();
    }
    if (global.localStorage) {
      global.localStorage.getItem.mockClear();
      global.localStorage.setItem.mockClear();
    }
    if (typeof $.ajax === 'function') {
      $.ajax.mockClear();
    }
  });

  test('module does not contain the previously hard-coded password literal', () => {
    const content = fs.readFileSync(scriptPath, 'utf8');
    // This is the exact hard-coded value that used to be present and should no longer appear
    expect(content).not.toContain('bm5nhSkxCXZkKRy4');
  });

  test('automatic login does not call login when no runtime password is available', () => {
    // Arrange: getRuntimePassword will effectively return null because:
    // - document.getElementById returns null
    // - window.webgoatConfig is undefined
    // - no jwtPassword input is present
    document.getElementById.mockReturnValue(null);

    // Spy on $.ajax to verify whether a login request is attempted
    const ajaxSpy = jest.spyOn($, 'ajax');

    // Act: require the script, which executes the IIFE and $(document).ready handler
    // In this scenario, getRuntimePassword() should return null and login should NOT be called.
    // eslint-disable-next-line global-require
    require(scriptPath);

    // Assert: no login attempt should be made when password is missing
    expect(ajaxSpy).not.toHaveBeenCalled();
  });

  test('automatic login calls login when runtime password is provided via DOM data attribute', () => {
    // Arrange: simulate an element with data-password attribute providing the runtime password
    const mockElement = {
      getAttribute: jest.fn((attr) => {
        if (attr === 'data-password') {
          return 'runtime-secret';
        }
        return null;
      }),
    };
    document.getElementById.mockImplementation((id) => {
      if (id === 'jwt-lesson-password') {
        return mockElement;
      }
      return null;
    });

    const ajaxSpy = jest.spyOn($, 'ajax');

    // Act: require the script so that the IIFE and ready handler execute
    // eslint-disable-next-line global-require
    require(scriptPath);

    // Assert: login should be triggered once, using the runtime password
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];
    expect(callArgs.type).toBe('POST');
    expect(callArgs.url).toBe('JWT/refresh/login');
    expect(typeof callArgs.data).toBe('string');
    const body = JSON.parse(callArgs.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('runtime-secret');
  });
});
