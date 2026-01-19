// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: This test is designed to verify the changed behavior in LessonContentModel.js
// after the fix for inefficient regular expressions (potential ReDoS).

// Since the project uses AMD (RequireJS-style) modules in the original code,
// we will simulate the AMD environment minimally for testing purposes.

const { JSDOM } = require('jsdom');

// Minimal AMD-style loader to capture the module factory and instantiate it
let lessonContentModelFactory;
global.define = function (deps, factory) {
  // We ignore deps resolution and directly capture the factory for our tests.
  lessonContentModelFactory = factory(
    require('jquery'),
    require('underscore'),
    // Provide a minimal Backbone stub sufficient for this model
    (() => {
      const Backbone = { Model: function () {} };
      Backbone.Model.prototype.fetch = function () {
        return { done: () => {} };
      };
      Backbone.Model.extend = function (proto) {
        function Ctor() {}
        Ctor.prototype = Object.create(Backbone.Model.prototype);
        Object.assign(Ctor.prototype, proto);
        // Provide a simple event system for trigger / on if needed
        Ctor.prototype._events = {};
        Ctor.prototype.trigger = function (name) {
          if (this._events[name]) {
            this._events[name].forEach((cb) => cb.apply(this, Array.prototype.slice.call(arguments, 1)));
          }
        };
        Ctor.prototype.on = function (name, cb) {
          if (!this._events[name]) this._events[name] = [];
          this._events[name].push(cb);
        };
        Ctor.prototype.set = function (key, value) {
          if (!this.attributes) this.attributes = {};
          this.attributes[key] = value;
        };
        Ctor.prototype.get = function (key) {
          return this.attributes ? this.attributes[key] : undefined;
        };
        return Ctor;
      };
      return Backbone;
    })(),
    // HTMLContentModel base is not relevant for URL logic; provide minimal stub.
    (() => {
      const Base = function () {};
      Base.extend = function (proto) {
        function Ctor() {}
        Ctor.prototype = Object.create(Base.prototype);
        Object.assign(Ctor.prototype, proto);
        Ctor.extend = Base.extend;
        return Ctor;
      };
      return Base.extend({});
    })()
  );
};

// Load the updated module code so that our custom `define` is invoked.
require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel URL and pageNum derivation (ReDoS fix delta test)', () => {
  let LessonContentModel;

  beforeAll(() => {
    // Instantiate the model constructor from the captured factory
    LessonContentModel = lessonContentModelFactory;
  });

  function createModelWithUrl(url) {
    // Use jsdom to simulate document.URL
    const dom = new JSDOM(`<!doctype html><html><body></body></html>`, {
      url,
    });
    global.document = dom.window.document;
    global.window = dom.window;

    const model = new LessonContentModel();
    return model;
  }

  test('sets lessonUrl to the .lesson root and pageNum to numeric tail when present', () => {
    const url = 'http://example.com/lesson-path/sample.lesson/5';
    const model = createModelWithUrl(url);

    model.setContent('<div>content</div>');

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-path/sample.lesson');
    expect(model.get('pageNum')).toBe(5);
  });

  test('sets lessonUrl to URL up to .lesson and pageNum 0 when there is no numeric tail', () => {
    const url = 'http://example.com/lesson-path/sample.lesson';
    const model = createModelWithUrl(url);

    model.setContent('<div>content</div>');

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-path/sample.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('falls back to pageNum 0 when the last segment is non-numeric', () => {
    const url = 'http://example.com/lesson-path/sample.lesson/not-a-number';
    const model = createModelWithUrl(url);

    model.setContent('<div>content</div>');

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-path/sample.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('handles URLs without .lesson safely without throwing and sets pageNum 0', () => {
    const url = 'http://example.com/other-path/page/123';
    const model = createModelWithUrl(url);

    expect(() => model.setContent('<div>content</div>')).not.toThrow();
    // Without .lesson, lessonUrl should be the original URL
    expect(model.get('lessonUrl')).toBe(url);
    // Last segment is 123 so pageNum becomes 123
    expect(model.get('pageNum')).toBe(123);
  });
});
