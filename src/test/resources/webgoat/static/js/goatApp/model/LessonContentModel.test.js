// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Delta tests focused on the updated regex behavior in LessonContentModel.js.
//
// These tests verify that:
// - For URLs ending with ".lesson", lessonUrl is the URL up to ".lesson" and pageNum is 0.
// - For URLs ending with ".lesson/<1-4 digits>", lessonUrl is the base URL up to ".lesson"
//   and pageNum is the numeric suffix.
//
// NOTE: The relative require path is inferred from the given source path and may need
// adjustment depending on the actual test runner/module resolution configuration.

const Backbone = require('backbone');
const _ = require('underscore');

// Minimal AMD-style loader shim for the module under test.
// In the real project, this might be handled by RequireJS or a bundler.
// We simulate `define([...], factory)` by requiring the module and exporting the return value.
let LessonContentModel;

beforeAll(() => {
  // Shim global `define` to capture the factory.
  global.define = function (deps, factory) {
    // Resolve only the dependencies actually used in the factory
    const $ = {}; // jQuery is not used in regex logic, can be a stub
    const HTMLContentModel = Backbone.Model.extend({});
    LessonContentModel = factory($, _, Backbone, HTMLContentModel);
  };
  global.define.amd = {}; // Mark as AMD-compatible if needed by the module

  // Load the module under test; this will invoke our shimmed define()
  // Adjust the path if your test runner uses a different base directory.
  // TODO: Update the relative path if module resolution differs.
  // eslint-disable-next-line global-require, import/no-unresolved
  require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
});

afterAll(() => {
  delete global.define;
  delete global.document;
});

describe('LessonContentModel regex behavior (delta tests)', () => {
  test('URL ending with ".lesson" sets lessonUrl to base and pageNum to 0', () => {
    // Arrange
    const url = 'http://example.com/lesson-path/my-lesson.lesson';
    global.document = { URL: url };

    const model = new LessonContentModel();

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-path/my-lesson.lesson');
    expect(model.get('pageNum')).toBe(0);
    expect(contentLoadedSpy).toHaveBeenCalledTimes(1);
  });

  test('URL ending with ".lesson/<1-4 digits>" sets lessonUrl to base and pageNum to numeric suffix', () => {
    // Arrange
    const url = 'http://example.com/lesson-path/my-lesson.lesson/1234';
    global.document = { URL: url };

    const model = new LessonContentModel();

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-path/my-lesson.lesson');
    expect(model.get('pageNum')).toBe('1234'); // Captured group is a string
    expect(contentLoadedSpy).toHaveBeenCalledTimes(1);
  });

  test('URL ending with ".lesson/<more than 4 digits>" does not match page pattern and falls back to pageNum 0', () => {
    // This ensures the {1,4} constraint is enforced and the regex does not
    // over-accept longer numeric suffixes.
    const url = 'http://example.com/lesson-path/my-lesson.lesson/12345';
    global.document = { URL: url };

    const model = new LessonContentModel();

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    // The base lessonUrl should remain unchanged (no suffix stripped)
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-path/my-lesson.lesson/12345');
    // No 1–4 digit suffix match, so pageNum should be 0
    expect(model.get('pageNum')).toBe(0);
    expect(contentLoadedSpy).toHaveBeenCalledTimes(1);
  });
});
