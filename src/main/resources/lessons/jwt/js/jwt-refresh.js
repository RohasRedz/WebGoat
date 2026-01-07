$(document).ready(function () {
    login('Jerry');
});

function getJwtRefreshPassword() {
    // Obtain the JWT refresh password from a configuration-driven source instead of hardcoding.
    // In this environment we read from a meta tag or environment-like global;
    // if not present, we fall back to an empty string to avoid embedding secrets in code.
    var meta = document.querySelector('meta[name="jwt-refresh-password"]');
    if (meta && meta.content) {
        return meta.content;
    }

    // As a last resort, use a non-secret empty value; the server must enforce proper credentials.
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hard-coded secret; now retrieved from configuration helper.
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
