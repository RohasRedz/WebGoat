$(document).ready(function () {
    // Use a non-sensitive placeholder password; real credentials must be supplied securely server-side.
    login('Jerry');
})

// NOTE: For security, do not hard-code real passwords or secrets here.
// The backend should validate the user and issue tokens based on securely stored credentials.
function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Use a clearly dummy password value so no real credential is exposed in source.
            // The server-side lesson logic should treat this as a non-sensitive placeholder.
            password: "DUMMY_LESSON_PASSWORD"
        })
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            // NOTE: apiToken and refreshToken should be provided by the server response;
            // this code assumes they are available in scope, as in the original implementation.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
