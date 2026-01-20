const $ = require('jquery');

describe('jwt-refresh delta tests - hard-coded password removal and token updates', () => {
    let originalAjax;
    let originalWebgoat;
    let originalLocalStorage;

    beforeEach(() => {
        originalAjax = $.ajax;
        $.ajax = jest.fn();

        originalWebgoat = global.webgoat;
        global.webgoat = {
            config: {
                jwtDemoPassword: 'configured-secret'
            },
            customjs: {}
        };

        originalLocalStorage = global.localStorage;
        const store = {};
        global.localStorage = {
            getItem: jest.fn(key => store[key]),
            setItem: jest.fn((key, value) => { store[key] = value; })
        };

        jest.resetModules();
    });

    afterEach(() => {
        $.ajax = originalAjax;
        global.webgoat = originalWebgoat;
        global.localStorage = originalLocalStorage;
        jest.clearAllMocks();
    });

    test('login uses configured password instead of hard-coded literal', () => {
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        const loginCall = $.ajax.mock.calls[0][0];
        const data = JSON.parse(loginCall.data);

        expect(data.password).toBe('configured-secret');
        expect(data.password).not.toBe('bm5nhSkxCXZkKRy4');
    });

    test('newToken updates tokens only from server response', () => {
        $.ajax.mockImplementation((options) => {
            if (options && typeof options.success === 'function') {
                options.success({
                    access_token: 'new_access',
                    refresh_token: 'new_refresh'
                });
            }
            return { success: jest.fn() };
        });

        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        global.localStorage.getItem.mockImplementation(key =>
            key === 'refresh_token' ? 'old_refresh' : 'old_access'
        );

        const newTokenFn = global.newToken || (global.window && global.window.newToken);
        expect(typeof newTokenFn).toBe('function');

        newTokenFn();

        expect(global.localStorage.setItem).toHaveBeenCalledWith('access_token', 'new_access');
        expect(global.localStorage.setItem).toHaveBeenCalledWith('refresh_token', 'new_refresh');
    });
});
