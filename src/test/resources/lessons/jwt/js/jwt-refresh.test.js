// Derived test path: src/test/resources/lessons/jwt/js/jwt-refresh.test.js

/**
 * Delta tests for jwt-refresh.js focusing on:
 * - getUserPassword(): reads password from DOM and does not use a hard-coded value.
 * - login(): sends the password returned by getUserPassword in the AJAX payload.
 */

const jsdom = require("jsdom");
const { JSDOM } = jsdom;

describe("jwt-refresh security delta tests", () => {
  let window;
  let document;
  let $;

  beforeEach(() => {
    const dom = new JSDOM(
      "<!doctype html><html><body><input id='jwt-refresh-password' type='password' value='secret123'/></body></html>",
      {
        url: "http://localhost/WebGoat",
      }
    );
    window = dom.window;
    document = window.document;
    global.window = window;
    global.document = document;

    $ = require("jquery")(window);
    global.$ = $;
    global.webgoat = { customjs: {} };

    // Clear and set up localStorage polyfill
    Object.defineProperty(window, "localStorage", {
      value: (function () {
        let store = {};
        return {
          getItem(key) {
            return store[key] || null;
          },
          setItem(key, value) {
            store[key] = String(value);
          },
          clear() {
            store = {};
          },
        };
      })(),
      configurable: true,
    });
  });

  afterEach(() => {
    delete global.window;
    delete global.document;
    delete global.$;
    delete global.webgoat;
  });

  test("getUserPassword reads value from jwt-refresh-password input and is not hard-coded", () => {
    // Arrange
    const script = require("../../../../../main/resources/lessons/jwt/js/jwt-refresh.js");

    // Act
    const password = window.getUserPassword("Jerry");

    // Assert
    expect(password).toBe("secret123");
    expect(password).not.toBe("bm5nhSkxCXZkKRy4");
  });

  test("login sends derived password from getUserPassword in AJAX payload", () => {
    // Arrange
    // Spy on $.ajax to inspect outgoing request body
    const ajaxSpy = jest.spyOn($, "ajax").mockImplementation(() => {
      return {
        success: (cb) => {
          cb({ access_token: "at", refresh_token: "rt" });
        },
      };
    });

    require("../../../../../main/resources/lessons/jwt/js/jwt-refresh.js");

    // Act
    window.login("Jerry");

    // Assert
    expect(ajaxSpy).toHaveBeenCalledTimes(1);
    const callArgs = ajaxSpy.mock.calls[0][0];

    expect(callArgs.type).toBe("POST");
    expect(callArgs.url).toBe("JWT/refresh/login");
    const body = JSON.parse(callArgs.data);

    expect(body.user).toBe("Jerry");
    expect(body.password).toBe("secret123");
    expect(body.password).not.toBe("bm5nhSkxCXZkKRy4");

    ajaxSpy.mockRestore();
  });
});
