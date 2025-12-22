// Assumption: this test file is colocated under a Jest-enabled test root.
// Source under test:
// src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// TODO: adjust path if module resolution differs in the real project layout.
jest.mock('jquery', () => ({}));
jest.mock('underscore', () => ({
  extend: Object.assign,
}));
jest.mock('backbone', () => {
  const Model = function () {};
  Model.prototype.fetch = jest.fn(function (options) {
    // Simulate a jQuery-like deferred with done()
    return {
      done: (cb) => {
        cb('<html>dummy</html>');
        return this;
      },
    };
  });
  return { Model };
});

// Load the module under test after mocks.
let LessonContentModelFactory;
beforeAll(() => {
  // TODO: update require path to the actual AMD/UMD build if needed.
  LessonContentModelFactory = require('../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
});

/**
 * Delta tests for LessonContentModel focusing on:
 * - Input sanitization and length limiting for options.name.
 * - Safe, precompiled regex usage when parsing document.URL.
 */
describe('LessonContentModel (delta tests)', () => {
  test('loadData() should sanitize and bound lesson name before building urlRoot', () => {
    // Arrange
    const RawHTMLContentModel = Backbone.Model; // mocked above
    const LessonContentModel = LessonContentModelFactory || RawHTMLContentModel;
    const instance = new LessonContentModel();

    const longAndDangerousName =
      'LESSON<>?*"\'/\\' + 'x'.repeat(300); // includes disallowed chars and is >128

    // Spy on fetch to assert that urlRoot is set safely before network call.
    const fetchSpy = jest.spyOn(instance, 'fetch').mockImplementation(function (options) {
      // Assert inside spy: urlRoot must be encoded, length-bounded, and character-filtered.
      const urlRoot = instance.urlRoot;
      expect(urlRoot).toMatch(/\.lesson$/);
      expect(urlRoot.length).toBeLessThanOrEqual(128 + '.lesson'.length * 2); // conservative bound
      expect(urlRoot).not.toContain('<');
      expect(urlRoot).not.toContain('>');
      expect(urlRoot).not.toContain('"');
      expect(urlRoot).not.toContain("'");
      expect(urlRoot).not.toContain('*');
      return {
        done: (cb) => {
          cb('<html>dummy</html>');
          return instance;
        },
      };
    });

    // Act
    instance.loadData({ name: longAndDangerousName });

    // Assert
    expect(fetchSpy).toHaveBeenCalledTimes(1);
    fetchSpy.mockRestore();
  });

  test('setContent() should derive pageNum from URL using safe regex', () => {
    // Arrange
    const RawHTMLContentModel = Backbone.Model;
    const LessonContentModel = LessonContentModelFactory || RawHTMLContentModel;
    const instance = new LessonContentModel();

    const originalUrl = global.document && global.document.URL;
    // Simulate a URL matching the expected pattern
    global.document = { URL: 'http://example.com/Some.lesson/1234' };

    const loadedSpy = jest.fn();
    instance.on = jest.fn((event, cb) => {
      if (event === 'content:loaded') {
        loadedSpy.mockImplementation(cb);
      }
    });

    // Act
    instance.setContent('<html/>', true);

    // Assert
    // After setContent, content and pageNum should be set consistently.
    expect(instance.get('content')).toBe('<html/>');
    expect(instance.get('lessonUrl')).toBe('http://example.com/Some.lesson');
    expect(instance.get('pageNum')).toBe('1234');

    // Cleanup
    if (originalUrl) {
      global.document.URL = originalUrl;
    }
  });
});
