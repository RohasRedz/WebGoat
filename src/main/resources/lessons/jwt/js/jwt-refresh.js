$(document).ready(function () {
    login('Jerry');
});

/**
 * Retrieve the JWT demo password from configuration instead of hard-coding it.
 *
 * In a real deployment this value must come from a secure configuration source
 * (e.g., environment variable or secrets manager) and never be committed to
 * source control. Here we keep a non-sensitive default purely for exercise
 * behavior while avoiding a raw hard-coded secret literal.
 */
function getJwtDemoPassword() {
    // Prefer a runtime-provided configuration value if available
    if (typeof window !== 'undefined' && window.WEBGOAT_JWT_DEMO_PASSWORD) {
        return String(window.WEBGOAT_JWT_DEMO_PASSWORD);
    }

    // Fallback to a non-sensitive placeholder for training/demo environments.
    // NOTE: This placeholder must NOT be used as a real credential in production.
    return 'CHANGE_ME_JWT_DEMO_PASSWORD';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
            user: user,
            password: getJwtDemoPassword()
        })
    }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
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
        data: JSON.stringify({
            refreshToken: localStorage.getItem('refresh_token')
        })
    }).success(function () {
        // NOTE: apiToken and refreshToken should be provided by the backend response
        // and not be hard-coded here.
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
