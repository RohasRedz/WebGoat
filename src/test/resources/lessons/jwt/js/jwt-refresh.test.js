const $ = require('jquery');

describe('jwt-refresh delta tests for hard-coded password removal and token handling', () => {
    let originalAjax;

    beforeAll(() => {
        originalAjax = $.ajax;
    });

    afterAll(() => {
        $.ajax = originalAjax;
    });

    beforeEach(() => {
        // simple in-memory localStorage mock
        const store = {};
        global.localStorage = {
            getItem: (k) => store[k],
            setItem: (k, v) => { store[k] = v; },
            removeItem: (k) => { delete store[k]; },
            clear: () => { Object.keys(store).forEach(k => delete store[k]); }
        };
    });

    test('login should not send hard-coded password in request body', () => {
        // Arrange
        const calls = [];
        $.ajax = jest.fn((opts) => {
            calls.push(opts);
            return { success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) };
        });

        // Re-require module under test so it uses mocked $.ajax
        jest.isolateModules(() => {
            require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
        });

        // Assert
        expect($.ajax).toHaveBeenCalled();
        const body = JSON.parse(calls[0].data);
        expect(body).toHaveProperty('user');
        expect(body).not.toHaveProperty('password');
    });

    test('newToken should update tokens from server response instead of undefined vars', () => {
        // Arrange
        localStorage.setItem('access_token', 'oldAccess');
        localStorage.setItem('refresh_token', 'oldRefresh');

        $.ajax = jest.fn(() => {
            return {
                success: (cb) => cb({
                    access_token: 'newAccess',
                    refresh_token: 'newRefresh'
                })
            };
        });

        jest.isolateModules(() => {
            const mod = require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
            // Invoke newToken through global if not exported
            if (typeof global.newToken === 'function') {
                global.newToken();
            } else if (mod && typeof mod.newToken === 'function') {
                mod.newToken();
            }
        });

        // Assert
        expect(localStorage.getItem('access_token')).toBe('newAccess');
        expect(localStorage.getItem('refresh_token')).toBe('newRefresh');
    });
});
