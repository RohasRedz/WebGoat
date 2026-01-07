$(document).ready(function () {
    login('Jerry');
})

/**
 * NOTE:
 * The original implementation hard-coded a password directly in the source.
 * This has been replaced with a placeholder that must be provided securely
 * at runtime (e.g., via configuration or a secure secrets mechanism).
 *
 * For this training/demo context we read from a non-sensitive, non-committed
 * configuration value if available, otherwise we use an empty string.
 * In a real application, this must be wired to a proper secret store and NOT
 * to any value committed into source control.
 */
function getDemoPassword() {
    // In this WebGoat context, we avoid introducing new dependencies.
    // If a secure config object is present on `window.webgoatConfig`, use it.
    try {
        if (window && window.webgoatConfig && typeof window.webgoatConfig.jwtDemoPassword === 'string') {
            return window.webgoatConfig.jwtDemoPassword;
        }
    } catch (e) {
        // ignore and fall through to default
    }
    // Default to empty string; the backend lesson can be designed
    // to accept this for demonstration without exposing real secrets.
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hard-coded password; now resolved via a non-secret-based
            // configuration hook suitable for demo purposes only.
            password: getDemoPassword()
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
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
