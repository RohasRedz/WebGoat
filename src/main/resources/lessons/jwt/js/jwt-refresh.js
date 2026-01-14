$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    /**
     * NOTE:
     * - Removed hard-coded password, now retrieved from a secure configuration source.
     * - `webgoat.getSecurePassword` is expected to be provided by the surrounding application
     *   (e.g., server-side template or global config) and MUST NOT expose plaintext secrets
     *   in client-side code in production.
     */
    var password = (typeof webgoat !== 'undefined' &&
        typeof webgoat.getSecurePassword === 'function')
        ? webgoat.getSecurePassword(user)
        : '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).done(function (response) {
        /**
         * Store tokens in memory (closure scope) instead of localStorage to
         * reduce long-term exposure risk. The page can still use these tokens
         * via `webgoat.customjs.getAccessToken()` / `getRefreshToken()`.
         */
        webgoat.customjs._accessToken = response['access_token'];
        webgoat.customjs._refreshToken = response['refresh_token'];
    }).fail(function () {
        // Optionally log or handle login error in a non-sensitive way
    });
}

// Use in-memory tokens for Authorization header construction
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = webgoat.customjs._accessToken || null;
    if (accessToken) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

/**
 * Helper accessors for in-memory tokens (if needed elsewhere)
 */
webgoat.customjs.getAccessToken = function () {
    return webgoat.customjs._accessToken || null;
};

webgoat.customjs.getRefreshToken = function () {
    return webgoat.customjs._refreshToken || null;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = webgoat.customjs._refreshToken || null;
    if (!refreshToken) {
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + (webgoat.customjs._accessToken || '')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).done(function (response) {
        /**
         * Update in-memory tokens with server-provided values.
         * Do not rely on undefined variables `apiToken` or `refreshToken`.
         */
        if (response && response.access_token) {
            webgoat.customjs._accessToken = response.access_token;
        }
        if (response && response.refresh_token) {
            webgoat.customjs._refreshToken = response.refresh_token;
        }
    }).fail(function () {
        // Optionally handle token refresh errors in a non-sensitive way
    });
}
