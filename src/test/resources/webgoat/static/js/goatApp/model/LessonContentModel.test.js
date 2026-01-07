const jsdom = require("jsdom");
const { JSDOM } = jsdom;

// Minimal stubs for AMD dependencies
const _ = require("underscore");
const Backbone = require("backbone");

global.define = function (deps, factory) {
  // Simple AMD shim: immediately instantiate with real/stubbed deps.
  const args = deps.map((dep) => {
    if (dep === "jquery") return require("jquery")(new JSDOM(`<!doctype html><html><body></body></html>`).window);
    if (dep === "underscore") return _;
    if (dep === "backbone") return Backbone;
    if (dep === "goatApp/model/HTMLContentModel") {
      // Minimal HTMLContentModel that is a Backbone.Model subclass
      return Backbone.Model.extend({});
    }
    // TODO: extend if more deps appear in future
    return {};
  });
  module.exports = factory.apply(null, args);
};

require("../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js");

describe("LessonContentModel - regex hardening delta tests", () => {
  let LessonContentModel;
  let window;

  beforeAll(() => {
    // Require the module after our AMD shim is set up
    LessonContentModel = module.exports;
  });

  beforeEach(() => {
    const dom = new JSDOM(`<!doctype html><html><body></body></html>`, {
      url: "http://localhost/WebGoat.lesson"
    });
    window = dom.window;
    global.document = window.document;
  });

  function createModel() {
    return new LessonContentModel();
  }

  test("setContent sets lessonUrl to base .lesson URL without trailing path", () => {
    const model = createModel();

    // Simulate a URL with extra segments after .lesson
    window.document.location.href = "http://localhost/WebGoat.lesson/42/extra";
    model.setContent("<html>dummy</html>");

    const lessonUrl = model.get("lessonUrl");
    expect(lessonUrl).toBe("http://localhost/WebGoat.lesson");
  });

  test("setContent extracts pageNum when URL ends with .lesson/<page>", () => {
    const model = createModel();
    window.document.location.href = "http://localhost/WebGoat.lesson/123";

    model.setContent("<html>dummy</html>");

    expect(model.get("pageNum")).toBe("123");
  });

  test("setContent sets pageNum to 0 when URL has no numeric page suffix", () => {
    const model = createModel();
    // No trailing /<digits>
    window.document.location.href = "http://localhost/WebGoat.lesson";

    model.setContent("<html>dummy</html>");

    expect(model.get("pageNum")).toBe(0);
  });

  test("setContent handles up to 4-digit page numbers", () => {
    const model = createModel();
    window.document.location.href = "http://localhost/WebGoat.lesson/9999";

    model.setContent("<html>dummy</html>");

    expect(model.get("pageNum")).toBe("9999");
  });

  test("setContent does not mis-parse very long numeric segments (>4 digits)", () => {
    const model = createModel();
    window.document.location.href = "http://localhost/WebGoat.lesson/12345";

    model.setContent("<html>dummy</html>");

    // Regex only matches up to 4 digits; longer segment should not match and fall back to 0
    expect(model.get("pageNum")).toBe(0);
  });
});
