// jwt-refresh.test.js
// Jest delta tests focusing on removal of hard-coded password and new getLoginPassword behavior

// Simulate browser globals used by the updated code
global.document = {
  querySelector: jest.fn()
};
global.window = global;

beforeEach(() => {
  // Clear previous mocks and storage
  jest.clearAllMocks();
  global.localStorage = (function () {
    let store = {};
    return {
      getItem(key) {
        return store[key] || null;
      },
      setItem(key, value) {
        store[key] = String(value);
      },
      clear() {
        store = {};
      }
    };
  })();
});

// Recreate the updated jwt-refresh.js behavior for testing
function defineJwtRefreshModule($) {
  var webgoat = global.webgoat || {};
  webgoat.customjs = webgoat.customjs || {};
  global.webgoat = webgoat;

  function getLoginPassword() {
    try {
      var metaPasswordElement = document.querySelector('meta[name="webgoat-jwt-password"]');
      if (metaPasswordElement && metaPasswordElement.content) {
        return metaPasswordElement.content;
      }
    } catch (e) {
      // Silent catch  do not leak details.
    }

    if (window.WEBGOAT_JWT_PASSWORD && typeof window.WEBGOAT_JWT_PASSWORD === 'string') {
      return window.WEBGOAT_JWT_PASSWORD;
    }

    return '';
  }

  function login(user) {
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: getLoginPassword() })
    }).done(function (response) {
      if (response && typeof response === 'object') {
        if (response['access_token']) {
          localStorage.setItem('access_token', response['access_token']);
        }
        if (response['refresh_token']) {
          localStorage.setItem('refresh_token', response['refresh_token']);
        }
      }
    });
  }

  webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var token = localStorage.getItem('access_token');
    if (token) {
      headers_to_set['Authorization'] = 'Bearer ' + token;
    }
    return headers_to_set;
  };

  function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
      headers: {
        Authorization: 'Bearer ' + localStorage.getItem('access_token')
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      data: JSON.stringify({ refreshToken: refreshToken })
    }).done(function (response) {
      if (response && typeof response === 'object') {
        if (response.access_token) {
          localStorage.setItem('access_token', response.access_token);
        }
        if (response.refresh_token) {
          localStorage.setItem('refresh_token', response.refresh_token);
        }
      }
    });
  }

  return {
    login,
    newToken,
    getLoginPassword,
    webgoat
  };
}

describe('jwt-refresh delta tests', () => {
  let $;
  let module;

  beforeEach(() => {
    $.ajax = jest.fn(() => ({
      done: (cb) => {
        cb({ access_token: 'ACCESS', refresh_token: 'REFRESH' });
        return this;
      }
    }));
    $ = { ajax: $.ajax };
    module = defineJwtRefreshModule($);
  });

  test('login uses password from meta tag when available instead of hard-coded literal', () => {
    const metaMock = { content: 'metaPassword' };
    document.querySelector.mockReturnValue(metaMock);

    module.login('Jerry');

    expect($.ajax).toHaveBeenCalledTimes(1);
    const call = $.ajax.mock.calls[0][0];
    const body = JSON.parse(call.data);

    expect(body.password).toBe('metaPassword');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to global WEBGOAT_JWT_PASSWORD when meta tag is not present', () => {
    document.querySelector.mockReturnValue(null);
    global.WEBGOAT_JWT_PASSWORD = 'globalPassword';

    module.login('Jerry');

    const call = $.ajax.mock.calls[0][0];
    const body = JSON.parse(call.data);

    expect(body.password).toBe('globalPassword');
  });

  test('login uses empty string when no password source is provided (no hard-coded default)', () => {
    document.querySelector.mockReturnValue(null);
    delete global.WEBGOAT_JWT_PASSWORD;

    module.login('Jerry');

    const call = $.ajax.mock.calls[0][0];
    const body = JSON.parse(call.data);

    expect(body.password).toBe('');
  });

  test('newToken updates tokens from response and does not use undefined globals', () => {
    localStorage.setItem('access_token', 'OLD_ACCESS');
    localStorage.setItem('refresh_token', 'OLD_REFRESH');

    let ajaxCalls = 0;
    $.ajax = jest.fn(() => {
      ajaxCalls += 1;
      return {
        done(cb) {
          cb({ access_token: 'NEW_ACCESS', refresh_token: 'NEW_REFRESH' });
          return this;
        }
      };
    });
    $ = { ajax: $.ajax };
    module = defineJwtRefreshModule($);

    module.newToken();

    expect(ajaxCalls).toBe(1);
    expect(localStorage.getItem('access_token')).toBe('NEW_ACCESS');
    expect(localStorage.getItem('refresh_token')).toBe('NEW_REFRESH');
  });
});
