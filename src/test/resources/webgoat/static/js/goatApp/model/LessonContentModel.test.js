// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

/**
 * Delta tests for LessonContentModel.js focusing on the URL parsing and
 * regex changes that mitigate inefficient regular expression complexity.
 *
 * These tests verify that:
 * - lessonUrl and pageNum are derived using the new logic.
 * - The behavior for typical URLs remains correct.
 */

const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the module dependency.
class HTMLContentModel extends Backbone.Model {}

jest.mock('goatApp/model/HTMLContentModel', () => HTMLContentModel);

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeAll(() => {
    // Require the AMD-style module by simulating define/require
    global.define = function (deps, factory) {
      const $ = {}; // not used here
      const _ = { escape: (s) => s };
      const BackboneLocal = Backbone;
      LessonContentModel = factory($, _, BackboneLocal, HTMLContentModel);
    };
    // Load the module under test
    require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  beforeEach(() => {
    // Default URL before each test; overridden where needed
    Object.defineProperty(global, 'document', {
      value: { URL: 'http://localhost/WebGoat/lesson/12' },
      writable: true
    });
  });

  test('setContent derives lessonUrl and pageNum using new URL-based logic', () => {
    // Arrange
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert: pageNum extracted from trailing numeric segment
    expect(model.get('pageNum')).toBe(12);
    // Assert: lessonUrl normalized to base lesson path + .lesson
    expect(model.get('lessonUrl')).toBe('/WebGoat/lesson.lesson');
  });

  test('setContent falls back to default pageNum=0 when URL has no numeric suffix', () => {
    // Arrange
    document.URL = 'http://localhost/WebGoat/lesson';
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('pageNum')).toBe(0);
    expect(model.get('lessonUrl')).toBe('/WebGoat/lesson.lesson');
  });
});
