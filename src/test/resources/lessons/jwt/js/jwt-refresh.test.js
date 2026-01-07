describe('jwt-refresh (delta tests for getLessonPassword)', () => {
  let originalWindow;
  let getLessonPassword;

  beforeEach(() => {
    // Preserve original global window if present
    originalWindow = global.window;
    global.window = {};
    // Require the module fresh for each test
    jest.resetModules();
    const moduleExports = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust path if needed

    // The original file defines getLessonPassword in the global scope.
    // Depending on bundler/test setup, it might be exported or attached to window.
    // We try both and fall back to global.
    getLessonPassword =
      moduleExports.getLessonPassword ||
      (global.getLessonPassword ? global.getLessonPassword : undefined);

    // Sanity check to ensure we bound the function
    if (typeof getLessonPassword !== 'function') {
      throw new Error('getLessonPassword is not accessible for testing; adjust test wiring/path.');
    }
  });

  afterEach(() => {
    global.window = originalWindow;
  });

  test('getLessonPassword prefers window.WEBGOAT_CONFIG.JWT_REFRESH_PASSWORD when set', () => {
    // Arrange
    global.window.WEBGOAT_CONFIG = { JWT_REFRESH_PASSWORD: 'configured-secret' };

    // Act
    const result = getLessonPassword();

    // Assert
    expect(result).toBe('configured-secret');
  });

  test('getLessonPassword falls back to legacy password when config is absent', () => {
    // Arrange: ensure no config is present
    delete global.window.WEBGOAT_CONFIG;

    // Act
    const result = getLessonPassword();

    // Assert: current behavior still returns the legacy hard-coded value
    expect(result).toBe('bm5nhSkxCXZkKRy4');
  });
});
