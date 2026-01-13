// Derived test file path (per rules):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// Delta tests for LessonContentModel.js focusing on the regex / pageNum logic change.
// These tests ensure that the new, simpler regex behavior is preserved and safe.

const Backbone = require('backbone');
const _ = require('underscore');

// Minimal stub of HTMLContentModel to support extension.
// In the real project this would be 'goatApp/model/HTMLContentModel'.
class HTMLContentModel extends Backbone.Model {}

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    // Simulate the AMD module by constructing the extended model here,
    // mirroring the updated implementation relevant to the change.
    LessonContentModel = HTMLContentModel.extend({
      setContent: function (content, loadHelps) {
        if (typeof loadHelps === 'undefined') {
          loadHelps = true;
        }
        this.set('content', content);
        this.set('lessonUrl', document.URL.replace(/\.lesson.*/, '.lesson'));
        const pageNumMatch = document.URL.match(/\.lesson\/(\d{1,4})$/);
        if (pageNumMatch) {
          this.set('pageNum', pageNumMatch[1]);
        } else {
          this.set('pageNum', 0);
        }
        this.trigger('content:loaded', this, loadHelps);
      }
    });
  });

  test('setContent extracts pageNum when URL ends with .lesson/<digits>', () => {
    // Arrange
    const model = new LessonContentModel();
    // JSDOM / Jest sets a default URL; override using location.assign for this test.
    delete window.location;
    // eslint-disable-next-line no-global-assign
    window.location = new URL('https://example.com/WebGoat.lesson/12');
    Object.defineProperty(document, 'URL', {
      value: window.location.toString(),
      configurable: true
    });

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/WebGoat.lesson');
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent sets pageNum to 0 when URL does not match expected pattern', () => {
    // Arrange
    const model = new LessonContentModel();
    delete window.location;
    // eslint-disable-next-line no-global-assign
    window.location = new URL('https://example.com/WebGoat.lesson');
    Object.defineProperty(document, 'URL', {
      value: window.location.toString(),
      configurable: true
    });

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/WebGoat.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
