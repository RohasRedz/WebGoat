$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Retrieve password from a non-hardcoded source (e.g., configuration, secure storage)
    // For this exercise, expect it to be provided via a data-attribute on the HTML element.
    var $loginSource = $('#jwt-login-config');
    var password = $loginSource.data('jwtPassword');

    if (typeof password !== 'string' || !password.length) {
        // Fail securely if no password is configured; do NOT hard-code any fallback secret
        // In a real deployment, this should be wired to a secure configuration mechanism.
        console.error('JWT login password is not configured. Aborting login call.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Do not log tokens; just persist them locally as before
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
    // Note: getItem result is currently unused, but kept to preserve behavior
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
            // apiToken and refreshToken are assumed to be defined elsewhere in the application context
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
