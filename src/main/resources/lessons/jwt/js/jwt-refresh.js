"use strict";

// Validate a JWT without using the server-side validation
var parseJwt = function(token) {
    var base64Url = token.split('.')[1];
    var base64 = base64Url.replace('-', '+').replace('_', '/');
    return JSON.parse(window.atob(base64));
};

$(function () {
    // Tokens are obtained dynamically from the server; no credentials or secrets are hard-coded here.
    // Get the jwt and load it for display by the server side
    $.ajax({
        type: "GET",
        url: "/JWT/refresh/token",
        dataType: "json",
        success: function (data) {
            // Set the server side rendered token
            $("#tokenView").html(data.token);
            $("#tokenDecoded").text(JSON.stringify(parseJwt(data.token), null, 4));
            $("#refreshTokenDecoded").text(JSON.stringify(parseJwt(data.refreshToken), null, 4));
        }
    });

    $("#renew").click(function(event) {
        event.preventDefault();
        $.ajax({
            type: "POST",
            url: "/JWT/refresh/renew",
            dataType: "json",
            data: "refreshToken=" + $("#clientRefreshToken").val(),
            success: function (data) {
                // Set the server side rendered token
                $("#tokenView").html(data.token);
                $("#tokenDecoded").text(JSON.stringify(parseJwt(data.token), null, 4));
                $("#refreshTokenDecoded").text(JSON.stringify(parseJwt(data.refreshToken), null, 4));
            },
            error: function(jqXHR) {
                $('#message').html(jqXHR.responseText);
                $('#messagediv').show();
            }
        });
    });
});
