// Password for JWT refresh login must be provided at runtime via a secure, external mechanism.
// This function intentionally does not hardcode the password value.
function getJwtRefreshPassword() {
    // Expecting a non-hardcoded value to be injected at runtime.
    // For example, via a global configuration object or environment-specific bootstrap.
    // Fallback is intentionally empty to avoid embedding secrets in source.
    if (typeof window !== 'undefined' && window.webgoat && typeof window.webgoat.jwtRefreshPassword === 'string') {
        return window.webgoat.jwtRefreshPassword;
    }
    return '';
}

$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hardcoded password; now obtained from an external, non-hardcoded source.
            password: getJwtRefreshPassword()
        })
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
