// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

/**
 * Delta tests for LessonContentModel focusing on the safer URL and regex handling:
 * - Verifies that lessonUrl is derived without using an inefficient regex.
 * - Verifies that pageNum is correctly extracted using the simplified pattern.
 */

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');
const HTMLContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/HTMLContentModel'); // TODO: adjust relative path if project layout differs
const LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel'); // TODO: adjust module export if using AMD/RequireJS

describe('LessonContentModel URL and page parsing (delta tests)', () => {
  beforeAll(() => {
    // Simulate Backbone model extension if needed; in WebGoat this is usually handled by AMD loader.
    Backbone.$ = $;
  });

  test('setContent normalizes lessonUrl to end with .lesson and extracts numeric pageNum', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'https://example.com/WebGoat.lesson/12';
    Object.defineProperty(global, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    // Act
    model.setContent('<html>dummy</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/WebGoat.lesson');
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent sets pageNum to 0 when URL does not end with .lesson/<digits>', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'https://example.com/WebGoat.lesson';
    Object.defineProperty(global, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    // Act
    model.setContent('<html>dummy</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/WebGoat.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
