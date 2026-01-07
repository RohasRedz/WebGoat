const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal stub for HTMLContentModel to satisfy the AMD dependency.
const HTMLContentModel = Backbone.Model.extend({
  setContent: function () {}
});

// Simulate AMD define wrapper used by LessonContentModel.js
// We require the real module under test by constructing the same factory pattern.
function loadLessonContentModel() {
  const factory = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  // In the real code, define([...], factory) returns HTMLContentModel.extend(...)
  // Here, factory is assumed to already return the extended model.
  return factory($, _, Backbone, HTMLContentModel);
}

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;
  let model;

  beforeEach(() => {
    // JSDOM provides window and document; ensure document.URL is writable for tests.
    LessonContentModel = loadLessonContentModel();
    model = new LessonContentModel();
  });

  test('sets lessonUrl and pageNum for URL ending with .lesson/number', () => {
    const originalUrl = 'https://example.com/path/to/lesson.lesson/42';
    Object.defineProperty(document, 'URL', {
      value: originalUrl,
      configurable: true
    });

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('https://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe('42');
  });

  test('sets lessonUrl and pageNum=0 for URL ending with .lesson only', () => {
    const originalUrl = 'https://example.com/another.lesson';
    Object.defineProperty(document, 'URL', {
      value: originalUrl,
      configurable: true
    });

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('https://example.com/another.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('sets pageNum=0 when URL does not match expected pattern', () => {
    const originalUrl = 'https://example.com/no-lesson-here';
    Object.defineProperty(document, 'URL', {
      value: originalUrl,
      configurable: true
    });

    model.setContent('<html>content</html>', true);

    // lessonUrl will just be originalUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson'),
    // which in this case returns originalUrl unchanged (no .lesson segment).
    expect(model.get('lessonUrl')).toBe(originalUrl);
    expect(model.get('pageNum')).toBe(0);
  });
});
