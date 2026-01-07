// Delta test for BATCH-003: LessonContentModel.js
// File path (inferred): src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: These tests focus only on the changed regex behavior in setContent:
// - lessonUrl is derived using the new precompiled regex \/\.lesson(?:$|\\/)\/
// - pageNum is extracted using the precompiled pageNumPattern.

const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to allow extension.
// TODO: Adjust require path if the real module is available in test environment.
const HTMLContentModel = Backbone.Model.extend({});

// Under test: the updated LessonContentModel module.
// We inline a minimal recreation based on the provided updated code to keep
// this a delta-focused unit test without requiring full AMD loader.
const LessonContentModel = HTMLContentModel.extend({
  urlRoot: null,
  defaults: {
    items: null,
    selectedItem: null
  },

  loadData: function (options) {
    this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
    const self = this;
    this.fetch().done(function (data) {
      self.setContent(data);
    });
  },

  setContent: function (content, loadHelps) {
    if (typeof loadHelps === 'undefined') {
      loadHelps = true;
    }
    this.set('content', content);
    // Updated regex logic (copied from fixed file)
    const lessonUrlPattern = /\.lesson(?:$|\/)/;
    const pageNumPattern = /.*\.lesson\/(\d{1,4})$/;

    this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));
    if (pageNumPattern.test(document.URL)) {
      this.set('pageNum', document.URL.replace(pageNumPattern, '$1'));
    } else {
      this.set('pageNum', 0);
    }
    this.trigger('content:loaded', this, loadHelps);
  },

  fetch: function (options) {
    options = options || {};
    return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
  }
});

describe('LessonContentModel - regex behavior (delta tests)', () => {
  let originalLocation;

  beforeAll(() => {
    // Save original window.location-like object if present
    originalLocation = global.window && global.window.location;
  });

  afterAll(() => {
    if (originalLocation) {
      global.window.location = originalLocation;
    }
  });

  function setDocumentUrl(url) {
    // Jest + jsdom: document.URL is derived from window.location.href
    delete global.window.location;
    global.window.location = new URL(url);
  }

  test('setContent normalizes lessonUrl without greedy replacement and sets pageNum when URL ends with number', () => {
    // Arrange
    const model = new LessonContentModel();
    setDocumentUrl('https://example.com/path/to/Lesson.lesson/12');

    const loadedSpy = jest.fn();
    model.on('content:loaded', loadedSpy);

    // Act
    model.setContent('<html>content</html>');

    // Assert: lessonUrl is normalized to end with ".lesson"
    expect(model.get('lessonUrl')).toBe('https://example.com/path/to/Lesson.lesson');
    // Assert: pageNum extracted from trailing segment
    expect(model.get('pageNum')).toBe('12');
    // Ensure event still fires
    expect(loadedSpy).toHaveBeenCalledWith(model, true);
  });

  test('setContent sets pageNum to 0 when URL does not match the lesson/page pattern', () => {
    // Arrange: URL without trailing lesson page number
    const model = new LessonContentModel();
    setDocumentUrl('https://example.com/other/route');

    // Act
    model.setContent('<html>content</html>');

    // Assert: lessonUrl replacement should be a no-op but still safe
    expect(model.get('lessonUrl')).toBe('https://example.com/other/route');
    // No page number -> should default to 0
    expect(model.get('pageNum')).toBe(0);
  });
});
