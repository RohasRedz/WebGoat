/**
 * Delta tests for jwt-refresh.js focusing only on the secret-handling fix:
 * - Ensure login() sends a non-hard-coded password taken from DOM
 * - Ensure no hard-coded secret value is present in the request payload
 */

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: function (cb) {
      // Immediately call success callback with fake tokens
      cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
      return this;
    }
  }));
  const $ = function () {};
  $.ajax = ajaxMock;
  $.fn = {};
  return $;
});

describe('jwt-refresh login behavior (delta tests)', () => {
  let $;
  let originalDocument;

  beforeEach(() => {
    jest.resetModules();
    $ = require('jquery');

    // Mock global document with a configurable password field
    originalDocument = global.document;
    global.document = {
      getElementById: jest.fn()
    };

    // Mock localStorage
    global.localStorage = {
      data: {},
      setItem(key, value) {
        this.data[key] = value;
      },
      getItem(key) {
        return this.data[key];
      }
    };

    // Load the module under test, which will attach login() to global scope
    require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  });

  afterEach(() => {
    global.document = originalDocument;
    jest.resetModules();
  });

  it('uses password from DOM instead of hard-coded literal', () => {
    const passwordField = { value: 'dynamicSecret!' };
    global.document.getElementById.mockReturnValue(passwordField);

    // Call the global login function exposed by jwt-refresh.js
    // eslint-disable-next-line no-undef
    login('Jerry');

    expect(global.document.getElementById).toHaveBeenCalledWith('jwt-demo-password');

    // Verify that jQuery.ajax was called with a body containing the DOM-derived password
    expect($.ajax).toHaveBeenCalledTimes(1);
    const ajaxConfig = $.ajax.mock.calls[0][0];

    const payload = JSON.parse(ajaxConfig.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('dynamicSecret!');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  it('does not include the removed hard-coded password literal in the request payload', () => {
    const passwordField = { value: 'anotherPassword' };
    global.document.getElementById.mockReturnValue(passwordField);

    // eslint-disable-next-line no-undef
    login('Jerry');

    const ajaxConfig = $.ajax.mock.calls[0][0];
    const payload = JSON.parse(ajaxConfig.data);

    // Assert that the removed literal is nowhere in the serialized payload
    const serialized = JSON.stringify(payload);
    expect(serialized).not.toContain('bm5nhSkxCXZkKRy4');
  });
});
