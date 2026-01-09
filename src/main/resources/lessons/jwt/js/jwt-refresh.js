$(document).ready(function () {
    login('Jerry');
})

// NOTE: For secure deployments, ensure the backend issues tokens only after validating
// credentials provided via a secure, non-hardcoded channel. This client should not
// contain any hardcoded secrets or passwords.
function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
}

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
        function (response) {
            // Update tokens from secure backend response instead of undefined variables
            if (response && response['access_token']) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && response['refresh_token']) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    )
}
