// Assuming a Jest test located under src/test/resources/lessons/jwt/js/jwt-refresh.spec.js
// and using CommonJS-style requires if needed.
// TODO: Adjust path/require resolution to match actual build configuration.

describe('jwt-refresh.js delta tests', () => {
  let originalWindow;

  beforeEach(() => {
    originalWindow = global.window;
    global.window = {};
    jest.resetModules();
  });

  afterEach(() => {
    global.window = originalWindow;
    jest.resetModules();
  });

  test('uses configurable WEBGOAT_DEMO_PASSWORD instead of hard-coded literal', () => {
    // Arrange
    global.window.WEBGOAT_DEMO_PASSWORD = 'config-secret';

    // Act: require the module so that WEBGOAT_DEMO_PASSWORD is evaluated
    const fs = require('fs');
    const path = require('path');
    // TODO: Adjust this path according to actual project layout if different.
    const scriptPath = path.resolve(
      __dirname,
      '../../../main/resources/lessons/jwt/js/jwt-refresh.js'
    );
    const content = fs.readFileSync(scriptPath, 'utf-8');

    // Assert
    // Ensure the previous hard-coded literal is no longer present
    expect(content).not.toMatch(/"bm5nhSkxCXZkKRy4"/);

    // Ensure the configurable constant is defined and used
    expect(content).toMatch(/const\s+WEBGOAT_DEMO_PASSWORD/);
    expect(content).toMatch(/password:\s*WEBGOAT_DEMO_PASSWORD/);
  });
});
