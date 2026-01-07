// Jest delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and corrected token refresh behavior.

// TODO: Adjust import path to match actual project structure/build.
jest.mock('jquery', () => {
  const ajaxMock = jest.fn();
  const $ = {
    ajax: ajaxMock,
    readyCallbacks: [],
    // minimal ready shim
    ready: (fn) => $.readyCallbacks.push(fn),
  };
  // Allow $(document).ready(...)
  const wrapper = (arg) => {
    if (typeof arg === 'function') {
      $.readyCallbacks.push(arg);
    }
    return $;
  };
  wrapper.ajax = ajaxMock;
  wrapper.readyCallbacks = $.readyCallbacks;
  return wrapper;
});

const $ = require('jquery');

// Provide minimal webgoat.customjs namespace used in the file under test.
global.webgoat = { customjs: {} };

// Use jsdom to provide a window/localStorage environment.
const jsdom = require('jsdom');
const { JSDOM } = jsdom;

describe('jwt-refresh.js delta tests', () => {
  let ajaxMock;
  let window;

  beforeEach(() => {
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com',
    });
    window = dom.window;
    global.window = window;
    global.document = window.document;
    global.localStorage = window.localStorage;

    // Reset jQuery ajax mock
    ajaxMock = $.ajax;
    ajaxMock.mockReset();

    // Clear any previously registered ready callbacks
    $.readyCallbacks.length = 0;

    // Load the script under test fresh each time
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust path
    });
  });

  test('login uses non-hardcoded password via helper and stores tokens from response', () => {
    // Arrange
    const responses = [];
    ajaxMock.mockImplementation((options) => {
      // Capture payload to ensure no hardcoded secret is present.
      const body = JSON.parse(options.data);

      // Assert within mock: password must not be the old hard-coded value.
      expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
      // It should be some placeholder (exact value is less important than the
      // fact that it is not a real secret).
      expect(typeof body.password).toBe('string');

      // Simulate successful response with tokens.
      const response = {
        access_token: 'ACCESS_TOKEN_FROM_SERVER',
        refresh_token: 'REFRESH_TOKEN_FROM_SERVER',
      };
      responses.push(response);
      if (typeof options.success === 'function') {
        options.success(response);
      }
    });

    // Act: trigger document ready handlers which call login('Jerry')
    $.readyCallbacks.forEach((fn) => fn());

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const lastCall = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(lastCall.data);

    expect(payload.user).toBe('Jerry');
    expect(localStorage.getItem('access_token')).toBe(
      'ACCESS_TOKEN_FROM_SERVER'
    );
    expect(localStorage.getItem('refresh_token')).toBe(
      'REFRESH_TOKEN_FROM_SERVER'
    );
  });

  test('addBearerToken attaches Authorization header only when access_token exists', () => {
    // Arrange
    localStorage.setItem('access_token', 'SOME_ACCESS_TOKEN');

    // Require jwt-refresh.js in this isolatedModules block already happened in beforeEach,
    // and it should have defined webgoat.customjs.addBearerToken.

    // Act
    const headers = webgoat.customjs.addBearerToken();

    // Assert
    expect(headers).toHaveProperty(
      'Authorization',
      'Bearer SOME_ACCESS_TOKEN'
    );
  });

  test('newToken sends stored refresh token and updates tokens from server response', () => {
    // Arrange
    localStorage.setItem('access_token', 'OLD_ACCESS');
    localStorage.setItem('refresh_token', 'OLD_REFRESH');

    ajaxMock.mockImplementation((options) => {
      const body = JSON.parse(options.data);
      // Ensure the stored refresh token is sent
      expect(body.refreshToken).toBe('OLD_REFRESH');

      const response = {
        access_token: 'NEW_ACCESS',
        refresh_token: 'NEW_REFRESH',
      };

      if (typeof options.success === 'function') {
        options.success(response);
      }
    });

    // Obtain reference to newToken from the loaded script
    // eslint-disable-next-line global-require
    const scriptModule = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js'); // TODO: adjust path
    // If newToken is global (function in script), call via global scope
    const newToken = global.newToken || scriptModule.newToken || undefined;
    expect(typeof newToken).toBe('function');

    // Act
    newToken();

    // Assert: tokens should now be replaced with values from response
    expect(localStorage.getItem('access_token')).toBe('NEW_ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
  });
});
