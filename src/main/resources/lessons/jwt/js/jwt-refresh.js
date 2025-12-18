$(document).ready(function () {
    login('Jerry');
});

// NOTE: For security reasons, do NOT hard-code real passwords or secrets here.
// This placeholder value is non-functional and must be replaced by a secure,
// server-side authentication mechanism or a secure configuration source.
const JWT_DEMO_PASSWORD = 'CHANGE_ME_IN_SECURE_CONFIG';

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Previously: password: "bm5nhSkxCXZkKRy4"
        // Now: clearly non-secret placeholder constant, not a real credential.
        data: JSON.stringify({ user: user, password: JWT_DEMO_PASSWORD })
    }).success(
        function (response) {
            // Do not log tokens; just store them locally for the demo.
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
            // NOTE: apiToken and refreshToken here are assumed to be defined by the server
            // response handler in the real application flow.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
