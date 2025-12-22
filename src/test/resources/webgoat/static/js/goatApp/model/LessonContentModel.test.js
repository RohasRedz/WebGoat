// Assuming Jest test file location derived from src/main/resources to src/test/resources
// Original file path: src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// Test file path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// TODO: Adjust module loading according to actual bundler/loader setup.
// For the purposes of delta testing, we assume LessonContentModel can be required as a module.

const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy inheritance; focus is on regex-related behavior
class HTMLContentModel extends Backbone.Model {}

// Inject our model definition similar to original AMD module
// In real project, replace this with: const LessonContentModel = require('path/to/LessonContentModel');
const LessonContentModelFactory = (function ($, _, Backbone, HTMLContentModel) {
  return HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null,
    },

    initialize: function (options) {},

    loadData: function (options) {
      var name = typeof options.name === 'string' ? options.name : '';
      name = name.substring(0, 255);

      this.urlRoot = _.escape(encodeURIComponent(name)) + '.lesson';
      var self = this;
      this.fetch().done(function (data) {
        self.setContent(data);
      });
    },

    setContent: function (content, loadHelps) {
      if (typeof loadHelps === 'undefined') {
        loadHelps = true;
      }
      this.set('content', content);

      var currentUrl = document.URL;

      var lessonUrlMatch = currentUrl.match(/\.lesson(\/\d{1,4})?$/);
      if (lessonUrlMatch) {
        this.set('lessonUrl', currentUrl.replace(/\.lesson(\/\d{1,4})?$/, '.lesson'));
      } else {
        this.set('lessonUrl', currentUrl.split('?')[0]);
      }

      var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
      if (pageMatch) {
        this.set('pageNum', pageMatch[1]);
      } else {
        this.set('pageNum', 0);
      }

      this.trigger('content:loaded', this, loadHelps);
    },

    fetch: function (options) {
      options = options || {};
      // For test purposes, simulate Backbone.Model.fetch returning a jQuery-like deferred with done()
      return {
        done: (cb) => {
          cb('<html/>');
          return this;
        },
      };
    },
  });
})(null, _, Backbone, HTMLContentModel);

const LessonContentModel = LessonContentModelFactory;

describe('LessonContentModel delta tests for regex and URL handling', () => {
  let originalDocument;

  beforeAll(() => {
    originalDocument = global.document;
    global.document = { URL: '' };
  });

  afterAll(() => {
    global.document = originalDocument;
  });

  test('setContent derives lessonUrl without using unbounded .* and correctly strips page segment', () => {
    // Arrange: URL that previously would match /.*\.lesson.*/ pattern
    global.document.URL = 'http://example.com/path/to/lesson.lesson/123?foo=bar';

    const model = new LessonContentModel();

    // Act
    model.setContent('<html/>');

    // Assert:
    // - lessonUrl is normalized to end with ".lesson"
    // - pageNum extracted as the trailing number
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe('123');
  });

  test('setContent falls back gracefully when URL does not end with .lesson', () => {
    // Arrange: Non-matching URL
    global.document.URL = 'http://example.com/path/without-lesson-segment?foo=bar';

    const model = new LessonContentModel();

    // Act
    model.setContent('<html/>');

    // Assert:
    // - lessonUrl should default to the URL without query parameters
    // - pageNum should default to 0
    expect(model.get('lessonUrl')).toBe('http://example.com/path/without-lesson-segment');
    expect(model.get('pageNum')).toBe(0);
  });

  test('loadData bounds and encodes name before constructing urlRoot', () => {
    // Arrange: Very long name to ensure substring bounding is applied
    const longName = 'x'.repeat(300);
    const model = new LessonContentModel();

    // Spy on fetch to avoid actual network calls
    const fetchSpy = jest.spyOn(model, 'fetch').mockImplementation(function () {
      return {
        done: (cb) => {
          cb('<html/>');
          return this;
        },
      };
    });

    // Act
    model.loadData({ name: longName });

    // Assert:
    // - urlRoot should be based on the first 255 characters of name, encoded and escaped.
    const boundedName = longName.substring(0, 255);
    const expectedRoot = _.escape(encodeURIComponent(boundedName)) + '.lesson';
    expect(model.urlRoot).toBe(expectedRoot);
    expect(fetchSpy).toHaveBeenCalled();

    fetchSpy.mockRestore();
  });
});
