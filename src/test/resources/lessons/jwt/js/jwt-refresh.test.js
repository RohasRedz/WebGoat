/**
 * Delta tests for jwt-refresh.js focusing on the hard-coded password remediation:
 * - Verifies that getJwtDemoPassword prefers window.webgoatConfig.jwtDemoPassword when set.
 * - Verifies that the fallback value is a non-secret placeholder and not the original hard-coded secret.
 */

describe('jwt-refresh hard-coded password remediation (delta tests)', () => {
    let originalWebgoatConfig;
    let originalDocumentReady;
    let ajaxSpy;

    beforeEach(() => {
        originalWebgoatConfig = window.webgoatConfig;
        window.webgoatConfig = undefined;

        // Prevent the real document.ready from firing login() automatically in tests
        originalDocumentReady = $.fn.ready;
        $.fn.ready = function (handler) {
            // Do not execute handler automatically during tests
            return this;
        };

        ajaxSpy = jest.spyOn($, 'ajax').mockImplementation(() => ({
            success: (cb) => {
                cb({ access_token: 'token', refresh_token: 'refresh' });
                return { success: () => {} };
            }
        }));

        // Require the module under test after stubbing globals
        jest.resetModules();
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    });

    afterEach(() => {
        window.webgoatConfig = originalWebgoatConfig;
        $.fn.ready = originalDocumentReady;
        ajaxSpy.mockRestore();
        jest.resetModules();
    });

    it('should use window.webgoatConfig.jwtDemoPassword when provided', () => {
        // Arrange
        window.webgoatConfig = { jwtDemoPassword: 'CONFIG_FROM_ENV' };
        jest.resetModules();
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        // Act
        // Call login directly to bypass document.ready
        // eslint-disable-next-line no-undef
        login('Jerry');

        // Assert
        expect($.ajax).toHaveBeenCalledTimes(1);
        const callArgs = $.ajax.mock.calls[0][0];
        const body = JSON.parse(callArgs.data);
        expect(body.password).toBe('CONFIG_FROM_ENV');
    });

    it('should fall back to a non-secret placeholder when config is missing', () => {
        // Arrange
        window.webgoatConfig = undefined;
        jest.resetModules();
        require('../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

        // Act
        // eslint-disable-next-line no-undef
        login('Jerry');

        // Assert
        const callArgs = $.ajax.mock.calls[0][0];
        const body = JSON.parse(callArgs.data);

        // The original vulnerable hard-coded secret must no longer be present
        expect(body.password).toBe('CONFIGURE_JWT_DEMO_PASSWORD');
        expect(body.password).not.toBe('bm5nhSkxCXZkKRy4');
    });
});
