// Jest tests focusing on removal of hard-coded password and new password sourcing behavior.

jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    success: (cb) => {
      cb({ access_token: 'access', refresh_token: 'refresh' });
      return { success: () => {} };
    },
  }));

  const valMock = jest.fn();

  const $mock = (...args) => {
    if (args[0] === '#jwt-password') {
      return { val: valMock };
    }
    return {};
  };

  $mock.ajax = ajaxMock;
  $mock.__valMock = valMock;

  return $mock;
});

const $ = require('jquery');

describe('jwt-refresh delta tests', () => {
  let originalLocalStorage;

  beforeEach(() => {
    originalLocalStorage = global.localStorage;
    const store = {};
    // simple mock localStorage
    global.localStorage = {
      setItem: (k, v) => {
        store[k] = v;
      },
      getItem: (k) => store[k],
    };
    jest.resetModules();
  });

  afterEach(() => {
    global.localStorage = originalLocalStorage;
  });

  test('login uses password from #jwt-password input when provided', () => {
    // Arrange
    const passwordValue = 'user-secret';
    $. __valMock.mockReturnValue(passwordValue);

    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // Assert
    const ajaxCalls = $.ajax.mock.calls;
    expect(ajaxCalls.length).toBeGreaterThan(0);
    const payload = JSON.parse(ajaxCalls[0][0].data);
    expect(payload.password).toBe(passwordValue);
  });

  test('login no longer sends hard-coded password literal', () => {
    const passwordValue = 'dynamic-pass';
    $. __valMock.mockReturnValue(passwordValue);

    jest.isolateModules(() => {
      require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    // Assert: ensure no hard-coded password literal is used
    const ajaxCalls = $.ajax.mock.calls;
    expect(ajaxCalls.length).toBeGreaterThan(0);
    const payload = JSON.parse(ajaxCalls[0][0].data);

    expect(payload.password).toBe(passwordValue);
    expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
