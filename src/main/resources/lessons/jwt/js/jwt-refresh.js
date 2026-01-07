$(document).ready(function () {
    login('Jerry');
});

function getJwtPassword() {
    // Read the password/secret from a runtime configuration source to avoid hard-coding it.
    // Fallback to an empty string if not configured; callers can handle failure explicitly.
    if (typeof window !== 'undefined' && window.WEBGOAT_CONFIG && window.WEBGOAT_CONFIG.JWT_PASSWORD) {
        return window.WEBGOAT_CONFIG.JWT_PASSWORD;
    }
    if (typeof process !== 'undefined' && process.env && process.env.WEBGOAT_JWT_PASSWORD) {
        return process.env.WEBGOAT_JWT_PASSWORD;
    }
    return '';
}

function login(user) {
    var password = getJwtPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: password})
    }).success(
        function (response) {
            if (response && typeof response === 'object') {
                if (response['access_token']) {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (response['refresh_token']) {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (accessToken) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
}

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({refreshToken: refreshToken})
    }).success(
        function (response) {
            // Use fresh tokens returned by the backend instead of undefined variables
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
