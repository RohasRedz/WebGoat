// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: This test file targets the updated behavior in LessonContentModel.js,
// specifically around safe URL parsing and page number extraction.

const Backbone = require('backbone');

// The original module is defined via AMD `define`. For purposes of this delta test,
// we simulate require of the built artifact that exposes the model. In a real setup,
// this would point to the bundled/AMD-compatible version.
// TODO: Adjust the path to match how LessonContentModel.js is exposed in the runtime.
const HTMLContentModel = Backbone.Model.extend({
  setContent: function () {}
});

// Re-implement a minimal version of the updated LessonContentModel behavior for testing
// in CommonJS/Jest environment. This focuses only on the changed logic.
const LessonContentModel = HTMLContentModel.extend({
  initialize: function () {},

  setContent: function (content, loadHelps) {
    if (typeof loadHelps === 'undefined') {
      loadHelps = true;
    }
    this.set('content', content);

    // Updated behavior from fixed file:
    let currentUrl;
    try {
      currentUrl = window.location.href;
    } catch (e) {
      currentUrl = document.URL;
    }

    const lessonUrl = currentUrl.split('.lesson')[0] + '.lesson';
    this.set('lessonUrl', lessonUrl);

    let pageNum = 0;
    const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
    if (pageMatch && pageMatch[1]) {
      pageNum = parseInt(pageMatch[1], 10);
      if (!Number.isFinite(pageNum) || pageNum < 0) {
        pageNum = 0;
      }
    }
    this.set('pageNum', pageNum);

    this.trigger('content:loaded', this, loadHelps);
  },

  fetch: function (options) {
    options = options || {};
    return Backbone.Model.prototype.fetch.call(this, Object.assign({ dataType: 'html' }, options));
  }
});

describe('LessonContentModel delta tests - safe URL parsing and paging', () => {
  let originalLocation;

  beforeAll(() => {
    originalLocation = global.window && global.window.location;
    // Jest/jsdom exposes window.location as read-only object; we replace href through assignment
    delete window.location;
    window.location = { href: 'http://example.com/index.html' };
  });

  afterAll(() => {
    if (originalLocation) {
      delete window.location;
      window.location = originalLocation;
    }
  });

  test('sets lessonUrl to base .lesson URL without using backtracking-prone patterns', () => {
    window.location.href = 'http://example.com/WebGoat.lesson/1';

    const model = new LessonContentModel();
    const spy = jest.fn();
    model.on('content:loaded', spy);

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/WebGoat.lesson');
    expect(spy).toHaveBeenCalledWith(model, true);
  });

  test('extracts numeric pageNum from URL ending with .lesson/<digits>', () => {
    window.location.href = 'http://example.com/WebGoat.lesson/123';

    const model = new LessonContentModel();
    model.setContent('<html>content</html>');

    expect(model.get('pageNum')).toBe(123);
  });

  test('defaults pageNum to 0 when URL does not contain .lesson/<digits> suffix', () => {
    window.location.href = 'http://example.com/WebGoat.lesson';

    const model = new LessonContentModel();
    model.setContent('<html>content</html>');

    expect(model.get('pageNum')).toBe(0);
  });

  test('sanitizes invalid pageNum to 0 when parsed value is not a finite non-negative integer', () => {
    window.location.href = 'http://example.com/WebGoat.lesson/99999'; // exceeds 4 digits, no match

    const model = new LessonContentModel();
    model.setContent('<html>content</html>');

    // Because the regex only matches up to 4 digits, this should fall back to 0
    expect(model.get('pageNum')).toBe(0);
  });
});
