$(document).ready(function () {
    login('Jerry');
});

function getWebGoatJwtPassword() {
    /*
     * Security fix:
     * - Remove hard-coded secret from source code (CWE-798).
     * - Use a non-sensitive, environment/config-driven value instead.
     * - For this exercise client code, we read from a well-known, non-secret
     *   configuration location if available, and otherwise fall back to a
     *   clearly non-production placeholder.
     *
     * Note:
     * - Real secrets must never live in front-end code; any value exposed
     *   here must be treated as non-secret. The real authentication secret
     *   must remain server-side (e.g., in server env vars / secret store).
     */
    if (typeof window !== 'undefined' &&
        window.webgoat &&
        window.webgoat.config &&
        typeof window.webgoat.config.jwtPassword === 'string' &&
        window.webgoat.config.jwtPassword.length > 0) {
        return window.webgoat.config.jwtPassword;
    }

    // Clearly non-secret placeholder for training/demo; must not be used in production.
    return 'CHANGE_ME_IN_CONFIG';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Use configuration-driven value instead of hard-coded secret
            password: getWebGoatJwtPassword()
        })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            // NOTE: apiToken and refreshToken should be provided by the server
            // response, not global variables. This logic is preserved as-is
            // because it is outside the reported vulnerability.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
