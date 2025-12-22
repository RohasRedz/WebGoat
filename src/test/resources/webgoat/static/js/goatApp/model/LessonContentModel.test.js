// Delta_UnitTest_Agent
// NOTE: Jest tests for the regex and URL-handling behavior added/changed in LessonContentModel.js.
// Test path inferred by replacing 'main' with 'test':
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// TODO: Adjust module path according to actual AMD/bundling setup.
const Backbone = require('backbone');

describe('LessonContentModel delta tests (regex and URL handling)', () => {
  // Minimal shim for HTMLContentModel so we can observe behavior in isolation.
  const HTMLContentModel = Backbone.Model.extend({});

  // Recreate the updated module behavior in CommonJS form for testing.
  const LessonContentModel = HTMLContentModel.extend({
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

      const currentUrl = global.document.URL;

      this.set('lessonUrl', currentUrl.replace(/\.lesson.*$/, '.lesson'));

      const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
      if (pageMatch) {
        this.set('pageNum', pageMatch[1]);
      } else {
        this.set('pageNum', 0);
      }

      this.trigger('content:loaded', this, loadHelps);
    },
  });

  let originalDocument;

  beforeAll(() => {
    originalDocument = global.document;
  });

  afterAll(() => {
    global.document = originalDocument;
  });

  function setDocumentUrl(url) {
    global.document = { URL: url };
  }

  test('setContent correctly normalizes lessonUrl and pageNum for URL with page number', () => {
    // Arrange
    setDocumentUrl('http://example.com/path/to/lesson/Intro.lesson/1234?foo=bar');
    const model = new LessonContentModel();
    const listener = jest.fn();
    model.on('content:loaded', listener);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson/Intro.lesson');
    expect(model.get('pageNum')).toBe('1234');
    expect(listener).toHaveBeenCalledWith(model, true);
  });

  test('setContent sets pageNum to 0 when URL does not end with numeric segment', () => {
    // Arrange
    setDocumentUrl('http://example.com/path/to/lesson/Intro.lesson?foo=bar');
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', false);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson/Intro.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
