$(document).ready(function () {
    // Use a non-secret placeholder here; real secret must be injected server-side or via configuration
    login('Jerry');
});

function login(user) {
    // Password is no longer hard-coded in the front-end.
    // It should be provided securely by the backend or via configuration, not in client code.
    var payload = {
        user: user
        // password: <removed hard-coded secret>
    };

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify(payload)
    }).success(
        function (response) {
            // Only store tokens if present and non-empty
            if (response && typeof response.access_token === 'string' && response.access_token.length > 0) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && typeof response.refresh_token === 'string' && response.refresh_token.length > 0) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }

    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Use stored refresh token if present, ensure safe access
    var refreshToken = localStorage.getItem('refresh_token');
    if (typeof refreshToken !== 'string' || refreshToken.length === 0) {
        return;
    }

    $.ajax({
        headers: webgoat.customjs.addBearerToken(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Update tokens only if the server returns them explicitly
            if (response && typeof response.access_token === 'string' && response.access_token.length > 0) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && typeof response.refresh_token === 'string' && response.refresh_token.length > 0) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
