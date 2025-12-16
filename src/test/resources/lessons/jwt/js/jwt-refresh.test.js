/**
 * Delta tests for jwt-refresh.js focusing only on:
 * - getLessonPassword() preferring DOM value over placeholder.
 * - login() sending a password value that is not a hard-coded literal.
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      // simulate success callback chainable style
      cb({ access_token: 'at', refresh_token: 'rt' });
      return { success: jest.fn() };
    },
  }));
  return {
    __esModule: true,
    default: {
      ajax: ajaxMock,
    },
    ajax: ajaxMock,
  };
});

const $ = require('jquery');

describe('jwt-refresh.js delta tests', () => {
  let originalDocument;
  let jwtModule;

  beforeEach(() => {
    jest.resetModules();
    originalDocument = global.document;

    // Minimal DOM emulation for getLessonPassword
    global.document = {
      getElementById: jest.fn(),
    };

    // Re-require the module under test after resetting modules/DOM
    jwtModule = require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust path if module system differs
  });

  afterEach(() => {
    global.document = originalDocument;
  });

  test('getLessonPassword prefers DOM value over placeholder', () => {
    // Arrange
    const inputElement = { value: 'fromDomPassword' };
    global.document.getElementById.mockReturnValue(inputElement);

    // Act
    const password = global.getLessonPassword
      ? global.getLessonPassword()
      : jwtModule.getLessonPassword
      ? jwtModule.getLessonPassword()
      : null; // TODO: adapt if exported differently

    // Assert
    expect(password).toBe('fromDomPassword');
  });

  test('getLessonPassword falls back to placeholder when DOM element missing or empty', () => {
    // Arrange: no element
    global.document.getElementById.mockReturnValue(null);

    // Act
    const passwordNoElement = global.getLessonPassword
      ? global.getLessonPassword()
      : jwtModule.getLessonPassword
      ? jwtModule.getLessonPassword()
      : null;

    // Assert
    expect(passwordNoElement).toBe('CHANGE_ME_LESSON_PASSWORD');

    // Arrange: empty value
    global.document.getElementById.mockReturnValue({ value: '' });

    const passwordEmpty = global.getLessonPassword
      ? global.getLessonPassword()
      : jwtModule.getLessonPassword
      ? jwtModule.getLessonPassword()
      : null;

    expect(passwordEmpty).toBe('CHANGE_ME_LESSON_PASSWORD');
  });

  test('login sends a password value derived from getLessonPassword, not a hard-coded literal', () => {
    // Arrange
    const user = 'Jerry';
    const domPassword = 'dynamicDomPass';
    global.document.getElementById.mockReturnValue({ value: domPassword });

    const ajaxSpy = $.ajax;

    // Act
    if (global.login) {
      global.login(user);
    } else if (jwtModule.login) {
      jwtModule.login(user);
    } else {
      // TODO: adapt to how login() is attached/exported
      throw new Error('login function not found for testing');
    }

    // Assert: ensure ajax called with JSON containing the dynamic password
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const call = ajaxSpy.mock.calls[0][0];
    expect(call.type).toBe('POST');
    expect(call.url).toBe('JWT/refresh/login');

    const body = JSON.parse(call.data);
    expect(body.user).toBe(user);
    expect(body.password).toBe(domPassword);

    // Assert that no known hard-coded literal remains
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
