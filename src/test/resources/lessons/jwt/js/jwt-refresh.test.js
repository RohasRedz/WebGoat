// File path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js
/* eslint-env jest */

jest.mock('jquery', () => {
  const actual = jest.requireActual('jquery');
  const $ = (...args) => actual(...args);
  $.ajax = jest.fn();
  $.fn = actual.fn;
  return $;
});

const $ = require('jquery');

describe('jwt-refresh login behavior', () => {
  beforeEach(() => {
    document.body.innerHTML = '<input id="jwt-password" value="runtimeSecret" />';
    $.ajax.mockReset();
    // Require the script after DOM is set so its ready handler can run
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });
  });

  test('login sends runtime-provided password and no hard-coded password', () => {
    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];

    const payload = JSON.parse(call.data);
    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('runtimeSecret');
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login uses provided password argument when called directly', () => {
    // Arrange: import the module and call login explicitly
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      const scriptModule = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
      if (typeof scriptModule.login === 'function') {
        $.ajax.mockReset();

        // Act
        scriptModule.login('Jerry', 'directPassword');

        // Assert
        expect($.ajax).toHaveBeenCalledTimes(1);
        const call = $.ajax.mock.calls[0][0];
        const payload = JSON.parse(call.data);
        expect(payload.user).toBe('Jerry');
        expect(payload.password).toBe('directPassword');
      }
    });
  });
});
