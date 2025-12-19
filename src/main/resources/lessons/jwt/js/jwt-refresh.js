$(document).ready(function () {
    login('Jerry');
});

function getJwtPassword() {
    // Retrieve the JWT password from a secure runtime source instead of hard-coding it.
    // For the lesson, we first try a non-source-controlled configuration object,
    // then fall back to an environment-like variable, and only as a last resort
    // use a benign placeholder value.
    if (window.webgoatConfig && typeof window.webgoatConfig.jwtPassword === 'string') {
        return window.webgoatConfig.jwtPassword;
    }

    if (typeof window.JWT_PASSWORD === 'string') {
        return window.JWT_PASSWORD;
    }

    // Fallback for misconfigured environments; keeps lesson behavior workable
    // without embedding real secrets in source.
    return 'CHANGE_ME_SECURELY_AT_RUNTIME';
}

function login(user) {
    var password = getJwtPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
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
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
