$(document).ready(function () {
    // NOTE: Do not hard-code passwords or secrets in source code.
    // The login password is now obtained at runtime from a secure input field.
    var defaultUser = 'Jerry';
    var passwordInput = $('#jwt-password').val();

    login(defaultUser, passwordInput);
});

function login(user, password) {
    // Ensure a non-empty password is provided at runtime
    if (!password) {
        // In a real application, handle this case gracefully (e.g., show validation message)
        // and never fall back to a hard-coded secret.
        console.error('Password must be provided at runtime, not hard-coded.');
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
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
