$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Retrieve password/secret from a safer configuration source instead of hard-coding
    // Fallback to an empty string if not configured so that no real secret is embedded in code.
    var passwordFromConfig = (window.webgoat && window.webgoat.config && typeof window.webgoat.config.jwtDemoPassword === 'string')
        ? window.webgoat.config.jwtDemoPassword
        : '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: passwordFromConfig })
    }).success(
        function (response) {
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // ensure we don't accidentally expose or misuse tokens; just read existing refresh token
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    $.ajax({
        headers: (function () {
            var headers = {};
            if (typeof accessToken === 'string' && accessToken.length > 0) {
                headers['Authorization'] = 'Bearer ' + accessToken;
            }
            return headers;
        })(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Update tokens only from server response to avoid using undefined variables
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
