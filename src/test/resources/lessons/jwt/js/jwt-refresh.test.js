// File: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/**
 * Delta tests for jwt-refresh.js focusing on:
 * - parseJwt correctly decoding a JWT payload
 * - #renew click handler wiring an AJAX POST to /JWT/refresh/renew
 */

jest.mock('jquery', () => {
  const clickHandlers = {};
  const mock$ = jest.fn((selector) => {
    if (selector === '#renew') {
      return {
        click: (handler) => {
          clickHandlers[selector] = handler;
        },
      };
    }
    if (selector === '#clientRefreshToken') {
      return {
        val: () => 'dummy-refresh-token',
      };
    }
    // For any other selector, just return a dummy object with chainable methods
    return {
      html: jest.fn(),
      text: jest.fn(),
      show: jest.fn(),
    };
  });
  mock$.ajax = jest.fn();
  mock$.clickHandlers = clickHandlers;
  return mock$;
});

const $ = require('jquery');

// Since the script uses window and document, provide minimal shims
global.window = {
  atob: (str) => Buffer.from(str, 'base64').toString('binary'),
};
global.document = {
  URL: 'http://localhost',
};

require('../../../../../main/resources/lessons/jwt/js/jwt-refresh');

describe('jwt-refresh.js delta tests', () => {
  test('parseJwt decodes valid JWT payload', () => {
    const header = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url');
    const payload = Buffer.from(JSON.stringify({ sub: 'user1', admin: true })).toString('base64url');
    const token = `${header}.${payload}.signature`;

    // Access parseJwt via global scope where the script defined it
    const parsed = global.parseJwt(token);

    expect(parsed).toEqual({ sub: 'user1', admin: true });
  });

  test('#renew click triggers AJAX POST to /JWT/refresh/renew with refreshToken', () => {
    const renewHandler = $.clickHandlers['#renew'];
    expect(typeof renewHandler).toBe('function');

    const fakeEvent = { preventDefault: jest.fn() };
    renewHandler(fakeEvent);

    expect(fakeEvent.preventDefault).toHaveBeenCalled();

    expect($.ajax).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'POST',
        url: '/JWT/refresh/renew',
        dataType: 'json',
        data: 'refreshToken=dummy-refresh-token',
      }),
    );
  });
});
