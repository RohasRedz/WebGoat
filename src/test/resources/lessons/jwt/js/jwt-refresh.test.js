// Delta tests focus on removal of hard-coded password and corrected newToken behavior

// Assume jwt-refresh.js registers functions on global scope / webgoat.customjs
require('../../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

describe('jwt-refresh.js - delta tests', () => {

    beforeEach(() => {
        // Reset localStorage mock
        global.localStorage = (function () {
            let store = {};
            return {
                getItem: key => store[key],
                setItem: (key, value) => { store[key] = value; },
                clear: () => { store = {}; }
            };
        })();

        global.$ = {
            ajax: jest.fn().mockReturnValue({
                success: function (cb) {
                    // Allow chaining behavior but callback can be triggered manually
                    this._successCb = cb;
                    return this;
                },
                triggerSuccess: function (response) {
                    if (this._successCb) this._successCb(response);
                }
            })
        };

        global.webgoat = { customjs: {} };
    });

    test('login no longer sends hard-coded password in request body', () => {
        // Arrange
        const ajaxMock = global.$.ajax;
        const testUser = 'Jerry';

        // Act
        // login is defined in jwt-refresh.js in the global scope
        login(testUser);

        // Assert
        expect(ajaxMock).toHaveBeenCalledTimes(1);
        const callArgs = ajaxMock.mock.calls[0][0];
        const body = JSON.parse(callArgs.data);

        expect(body.user).toBe(testUser);
        // Verify that the password field is not hard-coded or present
        expect(body.password).toBeUndefined();
    });

    test('newToken uses response tokens instead of undeclared globals', () => {
        // Arrange
        const ajaxCall = global.$.ajax();
        const response = {
            access_token: 'new-access',
            refresh_token: 'new-refresh'
        };

        global.localStorage.setItem('access_token', 'old-access');
        global.localStorage.setItem('refresh_token', 'old-refresh');

        // Act
        newToken();
        // Simulate successful response from server for the last ajax call
        ajaxCall.triggerSuccess(response);

        // Assert
        expect(global.localStorage.getItem('access_token')).toBe('new-access');
        expect(global.localStorage.getItem('refresh_token')).toBe('new-refresh');
    });
});
