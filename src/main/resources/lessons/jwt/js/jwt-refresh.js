$(document).ready(function () {
    // Do not embed credentials in front-end code; rely on server-side auth / demo-safe defaults
    login('Jerry');
});

function login(user) {
    // Password is no longer hard-coded; for this demo flow we send a non-sensitive placeholder
    // In a real system, this should come from a secure, server-side flow and never be embedded client-side.
    var loginPayload = {
        user: user,
        password: '' // intentionally blank to avoid hard-coded credentials in client code
    };

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify(loginPayload)
    }).success(
        function (response) {
            // Only store tokens if present; avoid logging or exposing them
            if (response && typeof response === 'object') {
                if (response.access_token) {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (response.refresh_token) {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Do not log tokens; just attach if present
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');

    $.ajax({
        headers: (function () {
            var token = localStorage.getItem('access_token');
            var hdrs = {};
            if (typeof token === 'string' && token.length > 0) {
                hdrs['Authorization'] = 'Bearer ' + token;
            }
            return hdrs;
        })(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Safely update tokens from the server response; avoid using undeclared variables
            if (response && typeof response === 'object') {
                if (response.access_token) {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (response.refresh_token) {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}
