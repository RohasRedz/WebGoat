require('jquery');

describe('jwt-refresh – removal of hard-coded password and use of configurable source', function () {
    beforeEach(() => {
        global.window = global.window || {};
        global.localStorage = (function () {
            let store = {};
            return {
                getItem: key => store[key],
                setItem: (key, value) => { store[key] = value; },
                clear: () => { store = {}; }
            };
        })();
        global.webgoat = { customjs: {} };
        document.body.innerHTML = '<div></div>';
        jest.resetModules();
    });

    function loadScript() {
        require('./jwt-refresh.js');
    }

    it('uses window.WEBGOAT_JWT_PASSWORD instead of hard-coded literal', function () {
        window.WEBGOAT_JWT_PASSWORD = 'dynamic-secret';
        const ajaxMock = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
        $.ajax = ajaxMock;

        loadScript();
        login('Jerry');

        expect(ajaxMock).toHaveBeenCalledTimes(1);
        const callArgs = ajaxMock.mock.calls[0][0];
        expect(callArgs.url).toBe('JWT/refresh/login');
        const payload = JSON.parse(callArgs.data);
        expect(payload.user).toBe('Jerry');
        expect(payload.password).toBe('dynamic-secret');
    });

    it('does not include the original hard-coded password in request payload', function () {
        window.WEBGOAT_JWT_PASSWORD = 'another-secret';
        const ajaxMock = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
        $.ajax = ajaxMock;

        loadScript();
        login('Jerry');

        const payload = JSON.parse(ajaxMock.mock.calls[0][0].data);
        expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
        expect(payload.password).toBe('another-secret');
    });

    it('falls back to empty string when WEBGOAT_JWT_PASSWORD is not set', function () {
        delete window.WEBGOAT_JWT_PASSWORD;
        const ajaxMock = jest.fn().mockReturnValue({ success: (cb) => cb({ access_token: 'a', refresh_token: 'r' }) });
        $.ajax = ajaxMock;

        loadScript();
        login('Jerry');

        const payload = JSON.parse(ajaxMock.mock.calls[0][0].data);
        expect(payload.password).toBe('');
    });
});
