// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Note: This test assumes an AMD-aware test environment or that the module is
// made available via a bundler under the same path used in the production code.

const jsdom = require("jsdom");
const { JSDOM } = jsdom;

// We require Backbone, underscore, and jQuery in the same way the production
// module expects them to be available globally.
const $ = require("jquery");
const _ = require("underscore");
const Backbone = require("backbone");

// In a typical WebGoat build, LessonContentModel is registered via RequireJS.
// For this delta test, we simulate that it is exported as a CommonJS module.
// If your build differs, adjust the require path accordingly.
const LessonContentModel = require("../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js");

describe("LessonContentModel URL helpers (delta tests)", () => {
  let window;
  let document;

  beforeEach(() => {
    const dom = new JSDOM("<!doctype html><html><body></body></html>", {
      url: "http://localhost/WebGoat/lesson/intro.lesson",
    });
    window = dom.window;
    document = window.document;

    global.window = window;
    global.document = document;
    global.$ = $(window);
    global.jQuery = global.$;
    global._ = _;
    global.Backbone = Backbone;
  });

  afterEach(() => {
    delete global.window;
    delete global.document;
    delete global.$;
    delete global.jQuery;
    delete global._;
    delete global.Backbone;
  });

  test("_computeLessonUrl returns base .lesson URL without path suffix", () => {
    // Arrange
    window.location.href =
      "http://localhost/WebGoat/lesson/Intro.lesson/3?foo=bar";
    const model = new LessonContentModel();

    // Act
    const baseUrl = model._computeLessonUrl();

    // Assert
    expect(baseUrl).toBe(
      "http://localhost/WebGoat/lesson/Intro.lesson"
    );
  });

  test("_computeLessonUrl falls back to full URL when no .lesson segment", () => {
    // Arrange
    window.location.href = "http://localhost/WebGoat/lesson/NoLessonHere";
    const model = new LessonContentModel();

    // Act
    const baseUrl = model._computeLessonUrl();

    // Assert
    expect(baseUrl).toBe("http://localhost/WebGoat/lesson/NoLessonHere");
  });

  test("_computePageNum extracts numeric page segment when present", () => {
    // Arrange
    window.location.href =
      "http://localhost/WebGoat/lesson/Intro.lesson/42";
    const model = new LessonContentModel();

    // Act
    const pageNum = model._computePageNum();

    // Assert
    expect(pageNum).toBe(42);
  });

  test("_computePageNum returns 0 when no .lesson/ segment is present", () => {
    // Arrange
    window.location.href = "http://localhost/WebGoat/lesson/Intro";
    const model = new LessonContentModel();

    // Act
    const pageNum = model._computePageNum();

    // Assert
    expect(pageNum).toBe(0);
  });

  test("_computePageNum returns 0 when non-numeric suffix follows .lesson/", () => {
    // Arrange
    window.location.href =
      "http://localhost/WebGoat/lesson/Intro.lesson/abc";
    const model = new LessonContentModel();

    // Act
    const pageNum = model._computePageNum();

    // Assert
    expect(pageNum).toBe(0);
  });
});
