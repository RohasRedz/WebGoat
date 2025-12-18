const { JSDOM } = require('jsdom');

// Load the script under test in a JSDOM environment
function loadJwtRefreshScript() {
  const dom = new JSDOM(`<!DOCTYPE html><html><head></head><body></body></html>`, {
    url: 'http://localhost/',
    runScripts: 'dangerously',
    resources: 'usable'
  });

  const { window } = dom;
  global.window = window;
  global.document = window.document;
  global.localStorage = (() => {
    let store = {};
    return {
      getItem: (k) => store[k] || null,
      setItem: (k, v) => { store[k] = String(v); },
      clear: () => { store = {}; }
    };
  })();

  // Minimal jQuery mock to intercept AJAX calls
  const ajaxMock = jest.fn().mockReturnValue({ success: (cb) => { cb({}); } });
  const $ = function () {};
  $.ajax = ajaxMock;
  $.fn = {};
  $.ready = jest.fn();
  $.prototype.ready = jest.fn();
  global.$ = $;

  // webgoat object used in the script
  global.webgoat = { customjs: {} };

  // Inject script contents (assumed to be available via require)
  // TODO: Adjust path if actual module system differs.
  const fs = require('fs');
  const path = require('path');
  const scriptPath = path.resolve(__dirname, '../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
  const scriptContent = fs.readFileSync(scriptPath, 'utf8');
  const scriptEl = window.document.createElement('script');
  scriptEl.textContent = scriptContent;
  window.document.head.appendChild(scriptEl);

  return { window, ajaxMock };
}

describe('jwt-refresh.js delta tests', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
    delete global.window;
    delete global.document;
    delete global.localStorage;
    delete global.$;
    delete global.webgoat;
  });

  test('login does not contain hard-coded secret literal and uses getJwtPassword()', () => {
    const fs = require('fs');
    const path = require('path');
    const scriptPath = path.resolve(__dirname, '../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const scriptContent = fs.readFileSync(scriptPath, 'utf8');

    // Assert that the previous hard-coded password string is gone
    expect(scriptContent).not.toMatch(/"bm5nhSkxCXZkKRy4"/);

    // Assert that getJwtPassword helper is present and used
    expect(scriptContent).toMatch(/function\s+getJwtPassword\s*\(/);
    expect(scriptContent).toMatch(/password:\s*getJwtPassword\(\)/);
  });

  test('getJwtPassword uses window.JWT_REFRESH_PASSWORD when provided', () => {
    const { window } = loadJwtRefreshScript();
    window.JWT_REFRESH_PASSWORD = 'CONFIG_DRIVEN_PASSWORD';

    // getJwtPassword is defined in global scope of script
    const getJwtPassword = window.getJwtPassword || global.getJwtPassword;
    expect(typeof getJwtPassword).toBe('function');

    const value = getJwtPassword();
    expect(value).toBe('CONFIG_DRIVEN_PASSWORD');
  });

  test('getJwtPassword falls back to non-secret placeholder when no config is present', () => {
    const { window } = loadJwtRefreshScript();

    const getJwtPassword = window.getJwtPassword || global.getJwtPassword;
    expect(typeof getJwtPassword).toBe('function');

    const value = getJwtPassword();
    // Placeholder as implemented in the fix
    expect(value).toBe('CHANGE_ME_NON_SECRET');
  });
});
