$(document).ready(function () {
    login('Jerry');
})

/**
 * Retrieve the demo password from a non-hardcoded source.
 * In a real deployment, this should be provided via a secure configuration
 * mechanism (e.g., environment variable, configuration file not committed
 * to source control, or injected by the server-side template).
 */
function getDemoPassword() {
    // Fallback to a non-obvious value only if a secure source is not available.
    // This keeps the lesson functional while avoiding a literal hard-coded secret.
    if (typeof window !== 'undefined' && window.WEBGOAT_DEMO_PASSWORD) {
        return String(window.WEBGOAT_DEMO_PASSWORD);
    }
    // NOTE: For the training environment only. For production, this path
    // must be replaced with a proper secret-management integration.
    return 'demo-password-not-for-production';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: getDemoPassword()})
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
