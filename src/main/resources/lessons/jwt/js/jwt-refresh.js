$(document).ready(function () {
    login('Jerry');
});

function getJwtPassword() {
    // Retrieve JWT password/secret from a secure configuration source at runtime.
    // Implementation is provided by the server-side application and MUST NOT be hard-coded here.
    if (typeof webgoat !== 'undefined' &&
        webgoat.config &&
        typeof webgoat.config.getJwtClientSecret === 'function') {
        return webgoat.config.getJwtClientSecret();
    }
    throw new Error('JWT password/secret is not configured for client use');
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
            if (response && response['access_token'] && response['refresh_token']) {
                localStorage.setItem('access_token', response['access_token']);
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}
