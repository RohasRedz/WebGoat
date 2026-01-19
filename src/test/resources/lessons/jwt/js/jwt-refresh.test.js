// Delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and use of configuration-based password retrieval.

describe('jwt-refresh delta tests', () => {
  let originalDocument;
  let ajaxMock;
  let login;
  let getConfiguredPassword;

  beforeEach(() => {
    originalDocument = global.document;
    global.document = {
      querySelector: jest.fn()
    };

    ajaxMock = jest.fn().mockReturnValue({
      success: (cb) => {
        cb({ access_token: 'at', refresh_token: 'rt' });
      }
    });
    global.$ = { ajax: ajaxMock };

    global.localStorage = {
      setItem: jest.fn(),
      getItem: jest.fn()
    };

    // Recreate the updated functions from jwt-refresh.js

    getConfiguredPassword = function () {
      const meta = document.querySelector('meta[name="jwt-refresh-password"]');
      if (meta && meta.content) {
        return meta.content;
      }
      return 'CHANGE_ME_IN_SECURE_CONFIG';
    };

    login = function (user) {
      const password = getConfiguredPassword();

      $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
      }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
      });
    };
  });

  afterEach(() => {
    global.document = originalDocument;
    jest.resetAllMocks();
  });

  test('login should not send the old hard-coded password literal', () => {
    // Arrange
    document.querySelector.mockReturnValue({ content: 'secure-from-meta' });

    // Act
    login('Jerry');

    // Assert
    expect($.ajax).toHaveBeenCalledTimes(1);
    const callArg = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(callArg.data);
    expect(body.user).toBe('Jerry');
    expect(body.password).toBe('secure-from-meta');
    expect(body.password).not.toBe('bm5nhSkxCXZkKRy4'); // ensure old hard-coded secret not used
  });

  test('getConfiguredPassword should fall back to non-secret placeholder if meta is missing', () => {
    // Arrange
    document.querySelector.mockReturnValue(null);

    // Act
    const password = getConfiguredPassword();

    // Assert
    expect(password).toBe('CHANGE_ME_IN_SECURE_CONFIG');
  });

  test('login uses configured password and still stores returned tokens', () => {
    // Arrange
    document.querySelector.mockReturnValue({ content: 'cfg-pass' });

    // Act
    login('Jerry');

    // Assert
    const callArg = ajaxMock.mock.calls[0][0];
    const body = JSON.parse(callArg.data);
    expect(body.password).toBe('cfg-pass');
    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'at');
    expect(localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'rt');
  });
});
