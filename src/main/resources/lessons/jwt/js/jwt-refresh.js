// In this lesson context, treat the password as a non-sensitive demo value and avoid hard-coding it inline.
// It is now defined as a single constant to make it easy to change or externalize in the future.
const WEBGOAT_DEMO_JWT_PASSWORD =
    (typeof window !== 'undefined' && window.WEBGOAT_DEMO_JWT_PASSWORD)
        ? window.WEBGOAT_DEMO_JWT_PASSWORD
        : 'demo-password-only-used-for-webgoat-lesson';

$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Use the demo password constant instead of a hard-coded literal
            password: WEBGOAT_DEMO_JWT_PASSWORD
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
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
