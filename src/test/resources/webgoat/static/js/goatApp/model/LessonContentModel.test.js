// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the AMD dependency.
// In the real project, this should import the actual module.
const HTMLContentModel = Backbone.Model.extend({});

// Shim for AMD define used in the source file
global.define = function (deps, factory) {
  const module = factory($, _, Backbone, HTMLContentModel);
  module.__esModule = true;
  module.default = module;
  moduleUnderTest.exports = module;
};

const moduleUnderTest = { exports: null };

// Load the module under test (will fill moduleUnderTest.exports via global.define)
require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel');

const LessonContentModel = moduleUnderTest.exports;

describe('LessonContentModel setContent delta tests', () => {
  let model;

  beforeEach(() => {
    model = new LessonContentModel();
  });

  test('setContent derives lessonUrl and pageNum when URL has page number', () => {
    const originalUrl = 'http://example.com/MyLesson.lesson/12';
    const oldUrl = global.document && global.document.URL;
    global.document = { URL: originalUrl };

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/MyLesson.lesson');
    expect(model.get('pageNum')).toBe('12');

    if (oldUrl !== undefined) {
      global.document.URL = oldUrl;
    }
  });

  test('setContent sets pageNum to 0 when URL has no page number', () => {
    const originalUrl = 'http://example.com/MyLesson.lesson';
    const oldUrl = global.document && global.document.URL;
    global.document = { URL: originalUrl };

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/MyLesson.lesson');
    expect(model.get('pageNum')).toBe(0);

    if (oldUrl !== undefined) {
      global.document.URL = oldUrl;
    }
  });
});
