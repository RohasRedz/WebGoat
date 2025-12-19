const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// TODO: Adjust the relative path if the test runner resolves modules differently.
const LessonContentModel = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel – regex behavior in setContent (delta test)', () => {
  let model;
  let originalLocation;

  beforeEach(() => {
    // Backbone Model requires a DOM-like environment; Jest + jsdom usually provides this.
    model = new LessonContentModel();
    originalLocation = global.window && global.window.location
      ? { href: window.location.href }
      : null;

    // Ensure document and window exist for document.URL usage
    if (!global.window) {
      global.window = {};
    }
    if (!global.document) {
      global.document = { URL: '' };
    }
  });

  afterEach(() => {
    // Restore original location if necessary
    if (originalLocation && global.window && global.window.location) {
      window.location.href = originalLocation.href;
    }
  });

  test('setContent correctly normalizes lessonUrl and extracts pageNum using regex', () => {
    const url = 'http://example.com/SomeLesson.lesson/12';
    document.URL = url;

    // Act
    model.setContent('<html>dummy</html>', true);

    // Assert: lessonUrl should strip the trailing /.lesson/<page> part to just .lesson
    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');

    // pageNum should be extracted as the numeric suffix
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent sets pageNum to 0 when URL does not match pattern', () => {
    const url = 'http://example.com/SomeLesson.lesson';
    document.URL = url;

    model.setContent('<html>dummy</html>', true);

    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
