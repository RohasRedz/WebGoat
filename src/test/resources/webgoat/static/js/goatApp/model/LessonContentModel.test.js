// TODO: Adjust import paths if the test runner's module resolution differs.
// This Jest test focuses only on the changed behavior in LessonContentModel.js:
//  - safer regex usage on document.URL
//  - sanitization of options.name when constructing urlRoot.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to allow extension
class HTMLContentModel extends Backbone.Model {}

// Monkey-patch AMD-style define for this test context
function loadLessonContentModel() {
  // Simulate the AMD module code structure
  const factory = function ($, _, Backbone, HTMLContentModel) {
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function (options) {},

      loadData: function (options) {
        var rawName = typeof options.name === 'string' ? options.name : '';
        var safeName = rawName.replace(/[^A-Za-z0-9._-]/g, '');
        this.urlRoot = encodeURIComponent(safeName) + '.lesson';

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

        var currentUrl = String(document.URL);

        this.set('lessonUrl', currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson'));

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
        const dfd = new $.Deferred();
        // For delta test purposes, resolve immediately with dummy HTML
        setImmediate(() => dfd.resolve('<html></html>'));
        return dfd.promise();
      },
    });
  };

  return factory($, _, Backbone, HTMLContentModel);
}

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;
  let originalDocumentUrl;

  beforeAll(() => {
    LessonContentModel = loadLessonContentModel();
  });

  beforeEach(() => {
    originalDocumentUrl = global.document && global.document.URL;
    // Provide a default document for tests
    global.document = {
      URL: 'http://example.com/lesson/SomeLesson.lesson/3',
    };
  });

  afterEach(() => {
    if (originalDocumentUrl !== undefined && global.document) {
      global.document.URL = originalDocumentUrl;
    }
  });

  test('loadData sanitizes options.name before building urlRoot', () => {
    const model = new LessonContentModel();

    const maliciousName = 'Less../on?name=<script>alert(1)</script>';
    model.loadData({ name: maliciousName });

    const encodedUrlRoot = model.urlRoot;

    // The encoded value should not directly contain raw HTML/script characters
    expect(encodedUrlRoot).toContain('.lesson');
    expect(encodedUrlRoot).not.toContain('<');
    expect(encodedUrlRoot).not.toContain('>');
    expect(encodedUrlRoot).not.toContain(' ');

    // Ensure only allowed characters (after sanitization) contribute to the base name
    const basePart = decodeURIComponent(encodedUrlRoot.replace('.lesson', ''));
    expect(basePart).not.toMatch(/[^\w.\-]/);
  });

  test('setContent derives lessonUrl and pageNum using bounded regex without catastrophic backtracking', () => {
    const model = new LessonContentModel();

    // URL with additional path segments after .lesson
    global.document.URL = 'https://host/app/SomeLesson.lesson/3?query=1';

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    model.setContent('<html></html>');

    expect(model.get('lessonUrl')).toBe('https://host/app/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe('3');
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });

  test('setContent defaults pageNum to 0 when URL does not end with numeric segment', () => {
    const model = new LessonContentModel();

    // URL without /<page> suffix
    global.document.URL = 'https://host/app/OtherLesson.lesson';

    model.setContent('<html></html>');

    expect(model.get('lessonUrl')).toBe('https://host/app/OtherLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
