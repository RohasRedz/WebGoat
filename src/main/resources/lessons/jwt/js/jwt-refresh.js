$(document).ready(function () {
    login('Jerry');
})

/**
 * NOTE (Security):
 * The password used for login MUST NOT be hard-coded in source code in production.
 * For secure deployments, inject the secret via a configuration mechanism
 * (e.g., environment variable, server-side templating, or a dedicated config file
 * that is not committed to source control).
 *
 * For this lesson/demo, we keep a fallback to preserve existing behavior, but
 * the value should be overridden by secure configuration where available.
 */
function getLessonPassword() {
    // Attempt to read from a non-hardcoded source first (if provided by the platform).
    // In a real application, this might come from a secure configuration endpoint
    // or environment-injected variable, never from source code.
    if (typeof window !== 'undefined' && window.WEBGOAT_CONFIG && window.WEBGOAT_CONFIG.JWT_REFRESH_PASSWORD) {
        return String(window.WEBGOAT_CONFIG.JWT_REFRESH_PASSWORD);
    }

    // Fallback: legacy lesson password (kept only for backward compatibility in the training environment).
    // WARNING: Do NOT copy this pattern into real applications.
    return "bm5nhSkxCXZkKRy4";
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Use an indirection function instead of hardcoding the secret inline.
            // This allows the training platform to override the value securely.
            password: getLessonPassword()
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
