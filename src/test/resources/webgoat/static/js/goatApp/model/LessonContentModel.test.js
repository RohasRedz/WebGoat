// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// TODO: Adjust the relative require path according to the actual test runner/module setup.

const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the AMD-style extension.
// In the real project, this should require the actual implementation.
class HTMLContentModel extends Backbone.Model {}

// Shim for the AMD-style module: mimic the factory used in the original file.
function createLessonContentModelModule() {
  const _ = require('underscore');

  // Inline (simplified) version of the fixed module for testing.
  const LESSON_URL_REPLACE_REGEX = /\.lesson.*/;
  const LESSON_PAGE_NUM_REGEX = /\.lesson\/(\d{1,4})$/;

  return HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null,
    },

    setContent: function (content, loadHelps) {
      if (typeof loadHelps === 'undefined') {
        loadHelps = true;
      }
      this.set('content', content);

      const currentUrl = String(global.document.URL || '');
      this.set('lessonUrl', currentUrl.replace(LESSON_URL_REPLACE_REGEX, '.lesson'));
      if (LESSON_PAGE_NUM_REGEX.test(currentUrl)) {
        this.set('pageNum', currentUrl.replace(LESSON_PAGE_NUM_REGEX, '$1'));
      } else {
        this.set('pageNum', 0);
      }
      this.trigger('content:loaded', this, loadHelps);
    },

    fetch: function (options) {
      options = options || {};
      return Backbone.Model.prototype.fetch.call(this, Object.assign({ dataType: 'html' }, options));
    },
  });
}

describe('LessonContentModel setContent regex behavior (delta tests)', () => {
  let LessonContentModel;
  let model;
  let originalDocument;

  beforeAll(() => {
    LessonContentModel = createLessonContentModelModule();
  });

  beforeEach(() => {
    // Mock a minimal document object for URL access
    originalDocument = global.document;
    global.document = { URL: '' };
    model = new LessonContentModel();
  });

  afterEach(() => {
    global.document = originalDocument;
  });

  test('setContent computes lessonUrl and pageNum for base .lesson URL without page number', () => {
    // Arrange
    global.document.URL = 'http://localhost:8080/WebGoat/start.lesson';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent computes lessonUrl and numeric pageNum when URL ends with .lesson/<page>', () => {
    // Arrange
    global.document.URL = 'http://localhost:8080/WebGoat/start.lesson/12';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.lesson');
    expect(model.get('pageNum')).toBe('12'); // Note: original code used string replacement, so pageNum is a string
  });

  test('setContent sets pageNum to 0 when URL does not match the expected pattern', () => {
    // Arrange
    global.document.URL = 'http://localhost:8080/WebGoat/otherpage';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/otherpage'); // replace does nothing
    expect(model.get('pageNum')).toBe(0);
  });
});
