$(document).ready(function () {
    login('Jerry');
})

/**
 * NOTE:
 * The original implementation hard-coded a password in client-side JavaScript,
 * which is insecure. The actual secret should be supplied by the backend and/or
 * derived via a secure flow (e.g., credential entry, OAuth, or a secure token),
 * not embedded in the front-end.
 *
 * To preserve behavior without exposing the literal secret in source control,
 * this version expects the backend to provide a non-sensitive login token or
 * a placeholder password value via a data attribute or a configuration object
 * rendered into the page.
 *
 * Fallback behavior:
 * - If no configured password/token is available, the request will be sent
 *   without a password field, and the server should reject unauthorized calls.
 */
function getLoginPassword() {
    try {
        // Preferred: server renders a non-secret placeholder or short-lived token
        // into the DOM, NOT an actual long-lived password.
        var el = document.querySelector('[data-jwt-refresh-password]');
        if (el && typeof el.getAttribute === 'function') {
            var value = el.getAttribute('data-jwt-refresh-password');
            if (typeof value === 'string' && value.length > 0) {
                return value;
            }
        }
    } catch (e) {
        // Swallow DOM access errors; fall through to undefined.
    }
    // No password available; return undefined so the field can be omitted
    return undefined;
}

function login(user) {
    var payload = { user: user };
    var password = getLoginPassword();
    if (typeof password === 'string' && password.length > 0) {
        // Only include password if a non-empty, non-secret placeholder/token is provided
        payload.password = password;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify(payload)
    }).success(
        function (response) {
            // Never log tokens; only store them in web storage as before.
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
    // NOTE: preserve original behavior, but avoid unused reads and rely on
    // stored tokens, without exposing them in logs or elsewhere.
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function (response) {
            // Use response tokens if provided; otherwise keep existing ones.
            if (response && response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    )
}
