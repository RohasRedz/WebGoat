const $ = require('jquery');
jest.mock('jquery');

// Small harness to load the module under test after jest.mock is set up.
function loadModule() {
  // Clear cache to ensure module is loaded with current mocks
  jest.resetModules();
  return require('../jwt-refresh.js'); // TODO: adjust relative path if layout differs
}

describe('jwt-refresh.js delta tests', () => {
  let ajaxMock;
  let readyCallback;

  beforeEach(() => {
    // Mock $.ajax and $(document).ready
    ajaxMock = jest.fn().mockReturnValue({ success: fn => fn({ access_token: 'a', refresh_token: 'r' }) });
    $.ajax.mockImplementation(ajaxMock);

    // $(document).ready(...)
    $.fn.ready = jest.fn((cb) => {
      readyCallback = cb;
    });

    // $('#jwt-password').val()
    $.fn.val = jest.fn();

    // global localStorage mock
    global.localStorage = {
      setItem: jest.fn(),
      getItem: jest.fn()
    };

    loadModule();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  function invokeLoginDirectly() {
    // The module registers $(document).ready(function() { login('Jerry'); });
    // We manually invoke the ready callback to simulate DOM ready
    if (typeof readyCallback === 'function') {
      readyCallback();
    }
  }

  test('login does not send a hard-coded password anymore', () => {
    // Arrange: simulate that #jwt-password has some runtime-provided value
    const pw = 'runtime-secret';
    $.fn.val.mockReturnValueOnce(pw);

    // Act
    invokeLoginDirectly();

    // Assert: AJAX payload should include the DOM password value, not a literal hard-coded string
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(ajaxConfig.data);

    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe(pw);
    // Assert that the previous hard-coded secret is not present anywhere
    expect(JSON.stringify(ajaxConfig)).not.toContain('bm5nhSkxCXZkKRy4');
  });

  test('login falls back to empty password when #jwt-password has no value', () => {
    // Arrange: simulate missing or empty DOM value
    $.fn.val.mockReturnValueOnce(undefined);

    // Act
    invokeLoginDirectly();

    // Assert
    expect(ajaxMock).toHaveBeenCalledTimes(1);
    const ajaxConfig = ajaxMock.mock.calls[0][0];
    const payload = JSON.parse(ajaxConfig.data);

    expect(payload.user).toBe('Jerry');
    expect(payload.password).toBe('');
  });
});
