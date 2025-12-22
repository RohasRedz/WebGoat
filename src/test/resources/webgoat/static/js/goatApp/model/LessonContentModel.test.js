// TODO: Adjust import paths if the AMD module is wrapped differently in the test environment.
const Backbone = require('backbone');
const _ = require('underscore');
const $ = require('jquery');

// Minimal stub for HTMLContentModel so that LessonContentModel.js can extend it.
class HTMLContentModel extends Backbone.Model {}

// Jest mock for requirejs-style define
jest.mock('goatApp/model/HTMLContentModel', () => HTMLContentModel);

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeAll(() => {
    // Simulate AMD define wrapper by requiring the compiled module entry point.
    // TODO: Adjust path to match bundler/test setup.
    LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  test('loadData sanitizes and bounds options.name before constructing urlRoot', () => {
    // Arrange
    const model = new LessonContentModel();
    const longAndDangerousName =
      '  ../some/very/long/name/with/illegal$chars?<>' +
      'x'.repeat(200);

    // Act
    model.loadData({ name: longAndDangerousName });

    // Assert
    const urlRoot = model.urlRoot;
    expect(urlRoot.endsWith('.lesson')).toBe(true);

    const encodedPart = urlRoot.replace(/\.lesson$/, '');
    const decodedSafeName = decodeURIComponent(encodedPart);

    // Ensures trimming occurred and illegal characters were stripped
    expect(decodedSafeName.startsWith('..someverylongnamewithillegalchars')).toBe(true);

    // Ensures max length enforcement (100 chars in production code)
    expect(decodedSafeName.length).toBeLessThanOrEqual(100);
    // Ensures only allowed characters remain
    expect(/^[a-zA-Z0-9._-]*$/.test(decodedSafeName)).toBe(true);
  });

  test('setContent uses simple, bounded regex for lessonUrl and pageNum', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://example.com/path/to/lesson.lesson/12';
    const originalLocation = global.location;

    // Simulate document.URL in Node/JSDOM
    delete global.location;
    global.location = { href: originalUrl };
    Object.defineProperty(global.document, 'URL', {
      value: originalUrl,
      configurable: true,
    });

    try {
      // Act
      model.setContent('<html>test</html>');

      // Assert
      const lessonUrl = model.get('lessonUrl');
      const pageNum = model.get('pageNum');

      expect(lessonUrl).toBe('http://example.com/path/to/lesson.lesson');
      expect(pageNum).toBe('12');
    } finally {
      // Cleanup
      global.location = originalLocation;
    }
  });
});
