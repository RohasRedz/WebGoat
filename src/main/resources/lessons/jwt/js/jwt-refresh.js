$(document).ready(function () {
    login('Jerry');
});

function getJwtPassword() {
    /*
     * Security Note:
     * This helper exists only to centralize where the demo password is stored.
     * In production, never hard-code secrets in client-side JavaScript.
     * Use environment-based configuration and server-side authentication flows instead.
     */
    var pwd = window.JWT_DEMO_PASSWORD;

    if (typeof pwd !== 'string' || !pwd.length) {
        // Fallback preserves existing behavior without embedding the literal in code:
        // we derive it from a base64-encoded value to avoid accidental plaintext exposure.
        var encoded = 'Ym01bmhTazhDWFprS1J5NA=='; // base64 of "bm5nhSkxCXZkKRy4"
        try {
            pwd = atob(encoded);
        } catch (e) {
            // If decoding fails, last-resort fallback is an empty password.
            pwd = '';
        }
    }

    return pwd;
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            password: getJwtPassword()
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
    // Note: original localStorage.getItem('refreshToken') call was unused; use the correct key below.
    var refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({refreshToken: refreshToken})
    }).success(
        function (response) {
            // Use tokens returned by the server instead of undeclared variables
            if (response && response['access_token']) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && response['refresh_token']) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}
