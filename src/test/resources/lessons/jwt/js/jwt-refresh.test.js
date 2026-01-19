const $ = require('jquery');
require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh security behavior', () => {
    beforeEach(() => {
        // reset localStorage before each test
        global.localStorage = {
            store: {},
            getItem(key) {
                return this.store[key] || null;
            },
            setItem(key, value) {
                this.store[key] = value;
            },
            removeItem(key) {
                delete this.store[key];
            }
        };
        jest.spyOn($, 'ajax').mockImplementation(() => ({
            success: (cb) => {
                cb({ access_token: 'access', refresh_token: 'refresh' });
                return { success: () => {} };
            }
        }));
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    test('login does not send a hard-coded password literal', () => {
        // Arrange
        const ajaxSpy = jest.spyOn($, 'ajax');

        // Act
        // login is defined in jwt-refresh.js in global scope
        login('Jerry');

        // Assert
        expect(ajaxSpy).toHaveBeenCalledTimes(1);
        const callArgs = ajaxSpy.mock.calls[0][0];
        const payload = JSON.parse(callArgs.data);

        // The password should be obtained via getUserPassword and not be a specific hard-coded secret
        expect(payload.user).toBe('Jerry');
        expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
    });
});
