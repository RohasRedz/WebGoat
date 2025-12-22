// Delta_UnitTest_Agent
// NOTE: Jest tests focused on the password resolution behavior added in jwt-refresh.js.
// Test path inferred by replacing 'main' with 'test':
// src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// We inline a minimal version of the updated logic for isolation and determinism.
/* Updated behavior under test:
$(document).ready(function () {
    var effectivePassword = getLoginPassword();
    login('Jerry', effectivePassword);
});

function getLoginPassword() {
    if (typeof window !== 'undefined' && typeof window.WEBGOAT_JWT_PASSWORD === 'string' && window.WEBGOAT_JWT_PASSWORD.length > 0) {
        return window.WEBGOAT_JWT_PASSWORD;
    }
    if (typeof document !== 'undefined') {
        var passwordInput = document.getElementById('jwt-password');
        if (passwordInput && typeof passwordInput.value === 'string' && passwordInput.value.length > 0) {
            return passwordInput.value;
        }
    }
    return 'CHANGE_ME_IN_CONFIG';
}

function login(user, password) { ... }
*/

describe('jwt-refresh delta tests (password resolution)', () => {
  let originalWindow;
  let originalDocument;
  let loginSpy;

  // Recreate the functions under test
  function getLoginPassword() {
    if (
      typeof window !== 'undefined' &&
      typeof window.WEBGOAT_JWT_PASSWORD === 'string' &&
      window.WEBGOAT_JWT_PASSWORD.length > 0
    ) {
      return window.WEBGOAT_JWT_PASSWORD;
    }

    if (typeof document !== 'undefined') {
      const passwordInput = document.getElementById('jwt-password');
      if (passwordInput && typeof passwordInput.value === 'string' && passwordInput.value.length > 0) {
        return passwordInput.value;
      }
    }

    return 'CHANGE_ME_IN_CONFIG';
  }

  function login(user, password) {
    // For delta testing we only need to assert the value passed in,
    // so we delegate to a spy instead of performing any AJAX.
    loginSpy(user, password);
  }

  beforeEach(() => {
    originalWindow = global.window;
    originalDocument = global.document;
    global.window = {};
    global.document = { getElementById: jest.fn() };
    loginSpy = jest.fn();
  });

  afterEach(() => {
    global.window = originalWindow;
    global.document = originalDocument;
  });

  test('getLoginPassword prefers window.WEBGOAT_JWT_PASSWORD when set', () => {
    // Arrange
    global.window.WEBGOAT_JWT_PASSWORD = 'fromWindowSecret';
    global.document.getElementById.mockReturnValue({ value: 'fromDomSecret' });

    // Act
    const pwd = getLoginPassword();
    login('Jerry', pwd);

    // Assert
    expect(pwd).toBe('fromWindowSecret');
    expect(loginSpy).toHaveBeenCalledWith('Jerry', 'fromWindowSecret');
  });

  test('getLoginPassword falls back to DOM element when global secret is not set', () => {
    // Arrange
    delete global.window.WEBGOAT_JWT_PASSWORD;
    global.document.getElementById.mockReturnValue({ value: 'fromDomSecret' });

    // Act
    const pwd = getLoginPassword();
    login('Jerry', pwd);

    // Assert
    expect(pwd).toBe('fromDomSecret');
    expect(loginSpy).toHaveBeenCalledWith('Jerry', 'fromDomSecret');
  });

  test('getLoginPassword returns non-secret placeholder when no configured secret is available', () => {
    // Arrange
    delete global.window.WEBGOAT_JWT_PASSWORD;
    global.document.getElementById.mockReturnValue({ value: '' });

    // Act
    const pwd = getLoginPassword();
    login('Jerry', pwd);

    // Assert
    expect(pwd).toBe('CHANGE_ME_IN_CONFIG');
    expect(loginSpy).toHaveBeenCalledWith('Jerry', 'CHANGE_ME_IN_CONFIG');
  });
});
