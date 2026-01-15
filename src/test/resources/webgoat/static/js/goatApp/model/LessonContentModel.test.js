// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: This test focuses only on the changed regex behavior around lessonUrl and pageNum.
const { JSDOM } = require('jsdom');

// Minimal AMD loader shim to load the module under test
function loadLessonContentModel(done) {
  const dom = new JSDOM(`<!doctype html><html><body></body></html>`, {
    url: 'http://localhost/WebGoat.lesson/12'
  });
  global.window = dom.window;
  global.document = dom.window.document;

  // Provide minimal Underscore and Backbone shims for the tested behavior
  const _ = {
    escape: (s) => s,
    extend: Object.assign
  };
  const Backbone = {
    Model: function () {},
    ModelPrototypeFetch: jest.fn()
  };
  Backbone.Model.prototype = {
    fetch: Backbone.ModelPrototypeFetch
  };

  // Mock define/require to emulate AMD
  global.define = function (deps, factory) {
    const $ = {}; // not used in tested path
    const HTMLContentModel = function () {};
    HTMLContentModel.extend = (spec) => {
      function Ctor() {
        this.attributes = {};
      }
      Ctor.prototype = {
        set: function (k, v) { this.attributes[k] = v; },
        get: function (k) { return this.attributes[k]; },
        trigger: jest.fn()
      };
      Object.assign(Ctor.prototype, spec);
      return Ctor;
    };
    const Module = factory($, _, Backbone, HTMLContentModel);
    done({ Module, Backbone });
  };

  // Load the actual module file
  // eslint-disable-next-line global-require, import/no-unresolved
  require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
}

describe('LessonContentModel regex behavior (delta tests)', () => {
  test('setContent should normalize lessonUrl and extract numeric pageNum using safe regex', (done) => {
    loadLessonContentModel(({ Module }) => {
      const model = new Module();

      const content = '<html>...</html>';
      model.setContent(content, true);

      expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat.lesson');
      expect(model.get('pageNum')).toBe('12');
      done();
    });
  });

  test('setContent should default pageNum to 0 when URL does not match expected pattern', (done) => {
    const dom = new JSDOM(`<!doctype html><html><body></body></html>`, {
      url: 'http://localhost/WebGoat.other'
    });
    global.window = dom.window;
    global.document = dom.window.document;

    loadLessonContentModel(({ Module }) => {
      const model = new Module();

      const content = '<html>...</html>';
      model.setContent(content, true);

      expect(model.get('pageNum')).toBe(0);
      done();
    });
  });
});
