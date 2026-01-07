$(document).ready(function () {
    // In a real application, the password MUST be provided securely by the user
    // (e.g., from a form input) and never hard-coded in client-side JavaScript.
    var $passwordInput = $('#jwt-password');

    if ($passwordInput.length === 0) {
        // Element not present; do not attempt automatic login.
        // In production, handle this case in the UI instead of relying on auto-login.
        console.warn('jwt-refresh: #jwt-password input not found; skipping automatic login.');
        return;
    }

    var passwordFromInput = $passwordInput.val();
    if (typeof passwordFromInput !== 'string' || !passwordFromInput.length) {
        // Password is empty; avoid sending an empty or invalid password.
        console.warn('jwt-refresh: No password provided in #jwt-password; skipping automatic login.');
        return;
    }

    login('Jerry', passwordFromInput);
});

/**
 * Login with a username and password.
 * NOTE: This function no longer hard-codes the password; it expects a value provided at runtime
 * (e.g., from a user input field). Do NOT pass secrets or passwords that are baked into source code.
 */
function login(user, password) {
    if (typeof password !== 'string' || !password.length) {
        // Fail fast without attempting a request when no password is supplied.
        // In this training context we just return; production code should handle this gracefully in the UI.
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Tokens must come from the trusted server response, not from any client-side constant.
            if (response && typeof response === 'object') {
                if (response.access_token) {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (response.refresh_token) {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
}

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Ensure we only ever send the refresh token retrieved from storage,
    // which should have been set from a trusted server response.
    var storedRefreshToken = localStorage.getItem('refresh_token');
    if (!storedRefreshToken) {
        console.warn('jwt-refresh: No refresh_token found in localStorage; cannot request new token.');
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: storedRefreshToken })
    }).success(
        function (response) {
            // NOTE: In a real implementation, new tokens MUST come from this server response.
            if (response && typeof response === 'object') {
                if (response.access_token) {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (response.refresh_token) {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}
