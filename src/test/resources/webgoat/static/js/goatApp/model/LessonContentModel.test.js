// Test file path derived from:
// src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// -> src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: This test assumes an AMD loader or build step that exposes the module as a CommonJS require.
// If that is not the case in the real project setup, adjust the import accordingly.
const { JSDOM } = require('jsdom');

// Minimal AMD-style loader shim to obtain the module under test.
// In a real setup, this would be handled by RequireJS or the project's bundler.
const fs = require('fs');
const vm = require('vm');
const path = require('path');

function loadLessonContentModelModule() {
  const filePath = path.join(
    __dirname,
    '../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'
  );
  const code = fs.readFileSync(filePath, 'utf8');

  const sandbox = {
    define: (deps, factory) => {
      // Very small shim: resolve only the dependencies actually used in tests.
      const $ = {};
      const _ = {
        escape: (s) => s, // not relevant for regex tests
      };
      const Backbone = {
        Model: function () {},
      };
      Backbone.Model.prototype.fetch = function () {
        return {
          done: (cb) => cb('<html></html>'),
        };
      };

      const HTMLContentModel = Backbone.Model.extend
        ? Backbone.Model
        : Backbone.Model; // placeholder; tests use only setContent behavior

      module.exports = factory($, _, Backbone, HTMLContentModel);
    },
    module: { exports: {} },
  };

  vm.createContext(sandbox);
  vm.runInContext(code, sandbox, { filename: filePath });

  return sandbox.module.exports || module.exports;
}

describe('LessonContentModel.setContent regex behavior (delta tests)', () => {
  let LessonContentModel;
  let model;

  beforeAll(() => {
    LessonContentModel = loadLessonContentModelModule();
  });

  beforeEach(() => {
    // Provide a minimal Backbone-like model API for testing
    const attributes = {};
    model = new LessonContentModel();
    model.set = (key, value) => {
      attributes[key] = value;
    };
    model.get = (key) => attributes[key];
    model.trigger = jest.fn();
  });

  test('setContent sets lessonUrl and pageNum=0 when URL has no page number', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Hello</p>`, {
      url: 'http://example.com/SomeLesson.lesson',
    });
    global.document = dom.window.document;

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe(
      'http://example.com/SomeLesson.lesson'
    );
    expect(model.get('pageNum')).toBe(0);
    expect(model.trigger).toHaveBeenCalledWith(
      'content:loaded',
      model,
      true
    );
  });

  test('setContent sets lessonUrl and pageNum when URL ends with page number', () => {
    const dom = new JSDOM(
      `<!DOCTYPE html><p>Hello</p>`,
      {
        url: 'http://example.com/SomeLesson.lesson/42',
      }
    );
    global.document = dom.window.document;

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe(
      'http://example.com/SomeLesson.lesson'
    );
    expect(model.get('pageNum')).toBe('42');
  });

  test('setContent uses efficient regex and does not over-consume URL suffix', () => {
    const longSuffix =
      '/very/long/path/with/many/segments/that/previously/could/be/overmatched';
    const dom = new JSDOM(
      `<!DOCTYPE html><p>Hello</p>`,
      {
        url: 'http://example.com/SomeLesson.lesson' + longSuffix,
      }
    );
    global.document = dom.window.document;

    model.setContent('<html/>');

    // The fixed regex should normalize to ".lesson" and not keep the long suffix.
    expect(model.get('lessonUrl')).toBe(
      'http://example.com/SomeLesson.lesson'
    );
  });
});
