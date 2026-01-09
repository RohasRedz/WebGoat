// Delta unit tests for jwt-refresh.js using Jest.
// Focus areas:
// - login() no longer sends a hardcoded password in the payload.
// - newToken() updates tokens from the backend response, not from undefined variables.
// - Both functions interact with localStorage and $.ajax as expected after the fix.

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  let originalAjax;

  beforeAll(() => {
    // Preserve original $.ajax to restore later.
    originalAjax = $.ajax;
  });

  beforeEach(() => {
    // Mock localStorage
    const store = {};
    global.localStorage = {
      getItem: jest.fn(key => store[key]),
      setItem: jest.fn((key, value) => {
        store[key] = value;
      })
    };

    // Jest mock for $.ajax
    $.ajax = jest.fn(() => ({
      success: function (cb) {
        // Allow chaining pattern: $.ajax(...).success(...)
        this._successCallback = cb;
        return this;
      },
      triggerSuccess: function (response) {
        if (this._successCallback) {
          this._successCallback(response);
        }
      }
    }));

    // Load the module under test after mocks are in place.
    jest.resetModules();
    require('../../../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterAll(() => {
    $.ajax = originalAjax;
  });

  test('login sends user only (no hardcoded password) in JSON payload', () => {
    // Arrange
    const ajaxInstance = $.ajax.mock.results[0]?.value;

    // Act
    // Call login directly; it is defined in the module global scope.
    // eslint-disable-next-line no-undef
    login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callConfig = $.ajax.mock.calls[0][0];

    expect(callConfig.type).toBe('POST');
    expect(callConfig.url).toBe('JWT/refresh/login');
    expect(callConfig.contentType).toBe('application/json');

    const payload = JSON.parse(callConfig.data);
    expect(payload).toEqual({ user: 'Jerry' });
    // Explicitly confirm removal of the previously hardcoded password field.
    expect(payload.password).toBeUndefined();

    // Simulate backend response and check token storage
    ajaxInstance.triggerSuccess({
      access_token: 'ACCESS',
      refresh_token: 'REFRESH'
    });

    expect(global.localStorage.setItem).toHaveBeenCalledWith('access_token', 'ACCESS');
    expect(global.localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'REFRESH');
  });

  test('newToken updates tokens from backend response instead of undefined variables', () => {
    // Arrange
    const firstAjaxInstance = $.ajax.mock.results[0]?.value;

    // Ensure there is an existing access token and refresh token
    global.localStorage.getItem.mockImplementation(key => {
      if (key === 'access_token') return 'OLD_ACCESS';
      if (key === 'refresh_token') return 'OLD_REFRESH';
      return null;
    });

    // Load module ensures newToken is defined
    // eslint-disable-next-line no-undef
    newToken();

    // Assert ajax call for newToken
    expect($.ajax).toHaveBeenCalledTimes(2);
    const newTokenCall = $.ajax.mock.calls[1][0];

    expect(newTokenCall.type).toBe('POST');
    expect(newTokenCall.url).toBe('JWT/refresh/newToken');

    const body = JSON.parse(newTokenCall.data);
    expect(body).toEqual({ refreshToken: 'OLD_REFRESH' });

    // Simulate backend returning new tokens and ensure localStorage is updated
    const secondAjaxInstance = $.ajax.mock.results[1]?.value;
    secondAjaxInstance.triggerSuccess({
      access_token: 'NEW_ACCESS',
      refresh_token: 'NEW_REFRESH'
    });

    expect(global.localStorage.setItem).toHaveBeenCalledWith('access_token', 'NEW_ACCESS');
    expect(global.localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'NEW_REFRESH');
  });
});
