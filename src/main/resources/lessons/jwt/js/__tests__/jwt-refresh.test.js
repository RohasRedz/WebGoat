/* eslint-env jest */

// Provide a mock implementation of $.ajax since the original code depends on jQuery.
global.$ = {
  ajax: jest.fn().mockReturnValue({
    success: function (cb) {
      // Immediately invoke callback with a fake response
      cb({ access_token: 'token', refresh_token: 'refresh' });
    },
  }),
};

global.console = global.console || {};
console.warn = console.warn || jest.fn();

describe('jwt-refresh.js delta security tests', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  test('login aborts when secure password source is not configured (getUserPassword throws)', () => {
    // Arrange
    const user = 'Jerry';

    jest.doMock('../jwt-refresh.js', () => {
      const originalModule = jest.requireActual('../jwt-refresh.js');
      return {
        __esModule: true,
        ...originalModule,
        getUserPassword: () => {
          throw new Error('Password must be provided via a secure mechanism, not hard-coded in the client.');
        },
      };
    });

    const module = require('../jwt-refresh.js');

    // Spy on console.warn to ensure the abort path is taken
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    // Act
    module.login(user);

    // Assert
    expect(global.$.ajax).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalled();
    const message = warnSpy.mock.calls[0][0];
    expect(message).toMatch(/password must not be hard-coded/i);
  });

  test('no hard-coded password literal is present in login payload', () => {
    // Arrange
    const user = 'Alice';
    const password = 'SomeDynamicSecret';

    jest.doMock('../jwt-refresh.js', () => {
      const originalModule = jest.requireActual('../jwt-refresh.js');
      return {
        __esModule: true,
        ...originalModule,
        getUserPassword: () => password,
      };
    });

    const module = require('../jwt-refresh.js');

    // Act
    module.login(user);

    // Assert
    expect(global.$.ajax).toHaveBeenCalledTimes(1);
    const callArgs = global.$.ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);
    expect(body.password).toBe(password);
  });
});
