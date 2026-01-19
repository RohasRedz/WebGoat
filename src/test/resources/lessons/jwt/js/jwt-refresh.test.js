/**
 * Delta tests for jwt-refresh.js focusing on the removal of the hard-coded password.
 * Verifies that:
 *  - login() no longer sends the literal password "bm5nhSkxCXZkKRy4"
 *  - login() uses getUserPassword() instead.
 */

const $ = require('jquery');

// Require the script so that login and getUserPassword are defined in the global scope.
// Adjust the path to match your module resolution if necessary.
require('../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh delta tests', () => {
    beforeEach(() => {
        // Mock jQuery.ajax
        jest.spyOn($, 'ajax').mockImplementation(() => ({
            success: function (cb) {
                // Simulate immediate success callback
                cb({ access_token: 'token', refresh_token: 'refresh' });
            }
        }));
    });

    afterEach(() => {
        jest.restoreAllMocks();
        // Clean up any tokens from localStorage
        if (typeof localStorage !== 'undefined') {
            localStorage.clear();
        }
    });

    test('login uses getUserPassword() instead of hard-coded password', () => {
        // Arrange
        const getUserPasswordSpy = jest.spyOn(global, 'getUserPassword').mockReturnValue('dynamic-password');

        // Act
        global.login('Jerry');

        // Assert
        expect(getUserPasswordSpy).toHaveBeenCalled();

        expect($.ajax).toHaveBeenCalledTimes(1);
        const ajaxArg = $.ajax.mock.calls[0][0];

        // Ensure request body no longer contains the old hard-coded password
        const sentBody = JSON.parse(ajaxArg.data);
        expect(sentBody.user).toBe('Jerry');
        expect(sentBody.password).toBe('dynamic-password');
        expect(sentBody.password).not.toBe('bm5nhSkxCXZkKRy4');
    });

    test('getUserPassword returns empty string by default (no hard-coded secret)', () => {
        // Act
        const value = global.getUserPassword();

        // Assert
        expect(value).toBe('');
    });
});
