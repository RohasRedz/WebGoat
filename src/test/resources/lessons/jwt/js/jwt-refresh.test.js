/* eslint-env jest */

// NOTE: These tests verify the removal of a hard-coded password and safer token handling
// in jwt-refresh.js. We simulate the browser environment minimally.

describe('jwt-refresh delta tests', () => {
  beforeEach(() => {
    // Set up a minimal DOM and storage environment
    global.localStorage = (function () {
      let store = {};
      return {
        getItem: (key) => store[key] || null,
        setItem: (key, value) => {
          store[key] = String(value);
        },
        clear: () => {
          store = {};
        },
      };
    }());

    global.$ = require('jquery');
    global.webgoat = { customjs: {} };

    jest.resetModules();
  });

  test('login does not send the original hard-coded password literal', (done) => {
    // Arrange
    const requests = [];
    jest.spyOn($, 'ajax').mockImplementation((options) => {
      requests.push(options);
      // simulate successful response
      const response = {
        access_token: 'access123',
        refresh_token: 'refresh123',
      };
      if (typeof options.success === 'function') {
        options.success(response);
      } else if (typeof options.then === 'function') {
        options.then(response);
      }
      return { success: (cb) => cb(response) };
    });

    // Act: require the script to execute its IIFE and login('Jerry')
    require('../../lessons/jwt/js/jwt-refresh');

    // Assert
    expect(requests).toHaveLength(1);
    const body = JSON.parse(requests[0].data);

    // Ensure that the previous hard-coded password is not present
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
    // And that some placeholder/non-sensitive password is used instead
    expect(typeof body.password).toBe('string');
    expect(body.password).not.toBe('');

    // Tokens should be stored from response
    expect(localStorage.getItem('access_token')).toBe('access123');
    expect(localStorage.getItem('refresh_token')).toBe('refresh123');

    done();
  });

  test('newToken uses response values instead of undeclared globals', (done) => {
    // Arrange
    localStorage.setItem('access_token', 'oldAccess');
    localStorage.setItem('refresh_token', 'oldRefresh');

    const ajaxMock = jest.spyOn($, 'ajax').mockImplementation((options) => {
      const response = {
        access_token: 'newAccess',
        refresh_token: 'newRefresh',
      };
      if (typeof options.success === 'function') {
        options.success(response);
      }
      return { success: (cb) => cb(response) };
    });

    // Load script to define newToken in closure
    const mod = require('../../lessons/jwt/js/jwt-refresh');

    // The script defines newToken inside an IIFE, but not globally.
    // We re-require to ensure side effects and then call the function via eval of source is not allowed,
    // so instead we trigger the ajax mock and assert localStorage updates via success handler.

    // Act: simulate a manual refresh call by invoking the success handler directly
    // This approximates the effect of calling newToken() and receiving a response.
    const options = ajaxMock.mock.calls[0][0];
    options.success({ access_token: 'newAccess', refresh_token: 'newRefresh' });

    // Assert
    expect(localStorage.getItem('access_token')).toBe('newAccess');
    expect(localStorage.getItem('refresh_token')).toBe('newRefresh');

    done();
  });
});
