// Resolved test file path (per instructions):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// We need to load the AMD-style module; in Jest we can simulate this by executing
// the module file in a context where `define` is available.
function loadLessonContentModel() {
  const fs = require('fs');
  const path = require('path');
  const vm = require('vm');

  const modulePath = path.resolve(
    __dirname,
    '../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'
  );
  const code = fs.readFileSync(modulePath, 'utf8');

  let ExportedModel = null;
  const sandbox = {
    define: (deps, factory) => {
      ExportedModel = factory($, _, Backbone, Backbone.Model);
    },
    require,
    console,
    module: {},
  };

  vm.runInNewContext(code, sandbox, { filename: modulePath });
  return ExportedModel;
}

describe('LessonContentModel regex behavior (delta tests)', () => {
  const LessonContentModel = loadLessonContentModel();

  test('normalizes lessonUrl to .lesson and sets pageNum from URL with page number', () => {
    // Arrange
    // URL with a page number at the end, e.g. /foo.lesson/12
    const originalUrl =
      'http://example.com/WebGoat/lesson/SomeLesson.lesson/12';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      originalUrl.replace(/\.lesson\/12$/, '.lesson')
    );
    expect(model.get('pageNum')).toBe('12');

    // Cleanup
    global.document = originalDocument;
  });

  test('sets pageNum to 0 when there is no trailing page number and normalizes lessonUrl', () => {
    // Arrange
    const originalUrl =
      'http://example.com/WebGoat/lesson/SomeLesson.lesson';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      originalUrl.replace(/\.lesson$/, '.lesson')
    );
    expect(model.get('pageNum')).toBe(0);

    // Cleanup
    global.document = originalDocument;
  });
});
