const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the module's dependency.
jest.mock('goatApp/model/HTMLContentModel', () => {
  const Model = Backbone.Model.extend({});
  return Model;
});

// Mock AMD define to allow requiring the module under Node/Jest.
function loadLessonContentModel() {
  let exported;
  global.define = function (deps, factory) {
    // Resolve minimal dependency set: jquery, underscore, backbone, HTMLContentModel
    const $ = require('jquery');
    const _ = require('underscore');
    const BackboneLocal = require('backbone');
    const HTMLContentModel = require('goatApp/model/HTMLContentModel');
    exported = factory($, _, BackboneLocal, HTMLContentModel);
  };
  // eslint-disable-next-line global-require
  require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  delete global.define;
  return exported;
}

describe('LessonContentModel.js delta tests', () => {
  let LessonContentModel;
  let originalLocation;

  beforeEach(() => {
    jest.resetModules();
    LessonContentModel = loadLessonContentModel();

    // Simulate browser-like location object
    originalLocation = global.location;
    global.location = { href: 'http://example.com' };

    // Provide a minimal document.URL shim
    global.document = {
      URL: 'http://example.com'
    };
  });

  afterEach(() => {
    global.location = originalLocation;
    delete global.document;
  });

  test('setContent derives lessonUrl and pageNum for basic .lesson URL without page number', () => {
    // Arrange
    const model = new LessonContentModel();
    global.document.URL = 'http://example.com/lesson1.lesson';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent derives lessonUrl and pageNum for .lesson URL with page number', () => {
    // Arrange
    const model = new LessonContentModel();
    global.document.URL = 'http://example.com/lesson1.lesson/12';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
    expect(model.get('pageNum')).toBe('12'); // extracted from regex capture group
  });

  test('setContent returns pageNum 0 when URL does not match .lesson/<digits> pattern', () => {
    // Arrange
    const model = new LessonContentModel();
    global.document.URL = 'http://example.com/otherpath';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/otherpath');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent handles longer complex URLs without catastrophic regex behavior', () => {
    // Arrange
    const model = new LessonContentModel();
    // Long URL with many path segments to mimic potential ReDoS input
    const longPath = '/a'.repeat(1000);
    global.document.URL = `http://example.com/lesson1.lesson${longPath}`;

    // Act
    // This call should complete promptly and still derive a valid lessonUrl without hanging.
    model.setContent('<html>content</html>');

    // Assert
    // The lessonUrl should stop at the first ".lesson"
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
    // No trailing page number pattern, so pageNum should remain 0
    expect(model.get('pageNum')).toBe(0);
  });
});
