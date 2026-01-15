$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Never hard-code passwords or secrets in client-side code.
        // The password is now intentionally omitted from this request payload.
        data: JSON.stringify({ user: user })
    }).success(
        function (response) {
            // Store tokens as before (this behavior is part of the lesson logic).
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
    // Removed unnecessary retrieval without usage to avoid confusion.
    // localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function (response) {
            // Use values coming from the server response instead of undefined variables.
            if (response && response['access_token']) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && response['refresh_token']) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}
