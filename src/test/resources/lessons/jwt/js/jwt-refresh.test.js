// Test file path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

// NOTE: Delta tests focusing on removal of hard-coded sensitive password.
// The updated code must use a clearly dummy placeholder and still invoke
// the login flow correctly.

const $ = require('jquery');
global.$ = $;

require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh login (delta tests)', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn($, 'ajax').mockImplementation(() => ({
            success: function (cb) {
                cb({ access_token: 'at', refresh_token: 'rt' });
                return this;
            }
        }));
        global.localStorage = {
            store: {},
            setItem(key, value) { this.store[key] = value; },
            getItem(key) { return this.store[key]; }
        };
    });

    it('uses a non-sensitive dummy password instead of the original hard-coded secret', () => {
        // Act
        // Call the globally defined login function from jwt-refresh.js
        global.login('Jerry');

        // Assert
        expect($.ajax).toHaveBeenCalledTimes(1);
        const callArgs = $.ajax.mock.calls[0][0];
        const payload = JSON.parse(callArgs.data);

        expect(payload.user).toBe('Jerry');
        // Ensures that the previously hard-coded password value is no longer used.
        expect(payload.password).toBe('DUMMY_LESSON_PASSWORD');
        expect(payload.password).not.toBe('bm5nhSkxCXZkKRy4');
    });
});
