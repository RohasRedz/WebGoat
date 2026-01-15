// Derived test file path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to allow extension
const HTMLContentModel = Backbone.Model.extend({});

// Import the updated module under test
// NOTE: In real setup, adjust the require path resolution to your module loader/bundler.
// Here we assume CommonJS loading from the same relative path.
const LessonContentModelFactory = require('./LessonContentModel.js');

describe('LessonContentModel – regex hardening', () => {
  test('setContent should derive lessonUrl without using greedy .* and handle pageNum correctly', () => {
    // Arrange
    const LessonContentModel = LessonContentModelFactory($, _, Backbone, HTMLContentModel);
    const model = new LessonContentModel();
    const originalUrl = 'http://example.com/course.lesson/12';
    const previousLocation = global.document && global.document.URL;
    global.document = { URL: originalUrl };

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/course.lesson');
    expect(model.get('pageNum')).toBe('12');

    // Cleanup
    if (previousLocation) {
      global.document.URL = previousLocation;
    }
  });

  test('setContent should set pageNum to 0 when URL has no trailing page segment', () => {
    // Arrange
    const LessonContentModel = LessonContentModelFactory($, _, Backbone, HTMLContentModel);
    const model = new LessonContentModel();
    const originalUrl = 'http://example.com/course.lesson';
    global.document = { URL: originalUrl };

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/course.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
