jest.mock('jquery', () => {
  const ajaxMock = jest.fn(() => ({
    done: (cb) => {
      cb({ access_token: 'access', refresh_token: 'refresh' });
    },
  }));
  return {
    ajax: ajaxMock,
    __esModule: true,
    default: { ajax: ajaxMock },
  };
});

const $ = require('jquery');

describe('jwt-refresh.js delta tests', () => {
  let originalWebgoat;
  let originalPrompt;

  beforeEach(() => {
    jest.resetModules();
    originalWebgoat = global.webgoat;
    originalPrompt = global.prompt;

    global.localStorage = {
      store: {},
      setItem(key, value) {
        this.store[key] = String(value);
      },
      getItem(key) {
        return this.store[key] || null;
      },
      clear() {
        this.store = {};
      },
    };
  });

  afterEach(() => {
    global.webgoat = originalWebgoat;
    global.prompt = originalPrompt;
    if (global.localStorage && typeof global.localStorage.clear === 'function') {
      global.localStorage.clear();
    }
  });

  test('login uses password from window.webgoat.getDemoPassword (no hard-coded literal)', () => {
    global.webgoat = {
      getDemoPassword: jest.fn(() => 'demo-pass'),
      customjs: {},
    };
    global.prompt = jest.fn();

    jest.isolateModules(() => {
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    login('Jerry');

    const callArgs = $.ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(global.webgoat.getDemoPassword).toHaveBeenCalled();
    expect(global.prompt).not.toHaveBeenCalled();
    expect(body.password).toBe('demo-pass');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to window.prompt when webgoat.getDemoPassword is unavailable', () => {
    global.webgoat = { customjs: {} };
    global.prompt = jest.fn(() => 'prompt-pass');

    jest.isolateModules(() => {
      require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    login('Jerry');

    const callArgs = $.ajax.mock.calls[0][0];
    const body = JSON.parse(callArgs.data);

    expect(global.prompt).toHaveBeenCalled();
    expect(body.password).toBe('prompt-pass');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });
});
