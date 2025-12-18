// File path assumption based on standard JS test layout:
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// NOTE: Adjust the relative path to jwt-refresh.js as needed in the actual project.
// This delta test focuses only on the changed behavior:
// - The login payload must not contain the old hardcoded password value.
const path = require('path');

// TODO: Adjust require path if test runner root differs.
const jwtRefreshPath = path.resolve(__dirname, '../../../main/resources/lessons/jwt/js/jwt-refresh.js');

// Jest does not execute jQuery/ajax in this isolated test; we will mock $.ajax
// and evaluate the payload it was called with.
describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    jest.resetModules();
    global.$ = {
      ajax: jest.fn().mockReturnValue({
        success: function (handler) {
          // For this delta test we don't need to invoke the handler.
          return this;
        }
      })
    };
    global.webgoat = { customjs: {} };
    global.localStorage = {
      store: {},
      setItem(key, value) { this.store[key] = value; },
      getItem(key) { return this.store[key]; }
    };
  });

  test('login payload must not contain the old hardcoded password literal', () => {
    // Arrange
    const OLD_PASSWORD = 'bm5nhSkxCXZkKRy4';

    // Require the script, which will execute $(document).ready and call login('Jerry')
    // with our mocked $ and document environment.
    global.document = { readyState: 'complete' };
    // Mock jQuery ready:
    global.$.ready = (fn) => fn();
    // Some jQuery builds use $(document).ready(fn), we simulate that through the mock:
    global.$.fn = { ready: (fn) => fn() };

    // Act
    // This will call login('Jerry') and in turn invoke $.ajax with new payload.
    require(jwtRefreshPath);

    // Assert
    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const ajaxArg = global.$.ajax.mock.calls[0][0];

    // Ensure the JSON payload can be parsed
    const payload = JSON.parse(ajaxArg.data);
    expect(payload).toHaveProperty('password');

    // Verify that the value is not the old hardcoded secret
    expect(payload.password).not.toBe(OLD_PASSWORD);

    // Additionally, ensure that a constant placeholder is used (non-empty, but clearly different)
    expect(typeof payload.password).toBe('string');
    expect(payload.password.length).toBeGreaterThan(0);
  });
});
