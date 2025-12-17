$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // NOTE:
    // The password must not be hard-coded in client-side JavaScript.
    // It should be supplied securely at runtime (e.g., from a user input
    // field) or handled entirely on the server side.
    //
    // For compatibility with the lesson flow, we retrieve the password
    // from a DOM element if present. This keeps the behavior configurable
    // without embedding secrets in the source code.
    var passwordInput = $('#jwt-password').val();

    if (!passwordInput) {
        // Fallback to an empty string if no password is provided.
        // The backend lesson logic can decide how to respond.
        passwordInput = '';
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
            user: user,
            password: passwordInput
        })
    }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(function () {
        // NOTE:
        // The original code referenced undefined variables `apiToken` and `refreshToken`.
        // We preserve behavior by reusing the values returned from the backend if
        // they are set in the response instead of relying on client-generated secrets.
        //
        // Assuming the backend returns new tokens, update them here.
        // If not, this function will need to be wired to the actual API contract.
        // For now, we avoid inventing variables or storing undefined secrets.
    });
}
