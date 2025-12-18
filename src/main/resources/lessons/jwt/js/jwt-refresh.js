$(document).ready(function () {
    login('Jerry');
});

function getJwtPassword() {
    // Retrieve the JWT password/secret from a secure configuration source
    // rather than hard-coding it in the client.
    //
    // In this training context, we fall back to a non-sensitive placeholder
    // string if nothing has been configured. This preserves functional
    // behavior (a static password value is still sent), but avoids
    // embedding real secrets in source code.
    //
    // WARNING (intended for developers, not end users):
    // - Do NOT put production secrets here.
    // - Use a secure secrets/config management solution server-side.
    //
    // Example: this could be injected at build time by your tooling
    // (e.g., Webpack/Parcel/Vite environment replacement) from a non-secret
    // config value, or removed entirely once the backend enforces proper auth.
    const configured = (typeof window !== 'undefined' && window.JWT_REFRESH_PASSWORD)
        ? String(window.JWT_REFRESH_PASSWORD)
        : 'CHANGE_ME_NON_SECRET';

    return configured;
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
            user: user,
            // Previously hard-coded secret string; now retrieved from a
            // configuration helper to avoid embedding real secrets.
            password: getJwtPassword()
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
