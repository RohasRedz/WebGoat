$(document).ready(function () {
    login('Jerry');
});

function getUserPassword(user) {
    // NOTE (Security Fix):
    // The password is intentionally NOT hard-coded anymore.
    // In a real deployment this MUST be provided securely, e.g.:
    // - Retrieved from a secure secrets manager
    // - Retrieved via a secure, authenticated API call
    // - Injected via environment-specific configuration
    //
    // For this lesson/demo, we keep a placeholder to preserve call structure.
    // Replace this implementation with a secure retrieval mechanism.
    throw new Error('Password must be provided via a secure mechanism, not hard-coded in the client.');
}

function login(user) {
    let password;
    try {
        password = getUserPassword(user);
    } catch (e) {
        // In a real application, you might show a generic error or route to a secure login page.
        // For this lesson context, we simply abort the request if no secure password is available.
        if (window.console && console.warn) {
            console.warn('Login aborted: password must not be hard-coded in the client.');
        }
        return;
    }

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
