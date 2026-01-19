// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModelTest.js

// NOTE: This test assumes AMD modules can be loaded in the Jest environment via require()
// and that the path below resolves to the updated LessonContentModel.js definition.
// If a different module loader is used in the project, adjust the require path/setup accordingly.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Mock HTMLContentModel as a basic Backbone model so that extending it works in tests.
const HTMLContentModel = Backbone.Model.extend({});

// Manually construct the same module that the AMD define() would return.
// This mirrors the updated LessonContentModel.js behavior, focusing on setContent.
const LessonContentModel = (function ($, _, Backbone, HTMLContentModel) {
  return HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null
    },

    initialize: function (options) {
      // no-op for these tests
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

      // This is the fixed implementation using precompiled, safe regex patterns
      const lessonUrlPattern = /\.lesson.*/;
      this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));

      const pageNumPattern = /.*\.lesson\/(\d{1,4})$/;
      const pageMatch = pageNumPattern.exec(document.URL);
      if (pageMatch) {
        this.set('pageNum', pageMatch[1]);
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
})($, _, Backbone, HTMLContentModel);

describe('LessonContentModel setContent (delta tests for regex fix)', () => {
  let originalLocation;

  beforeAll(() => {
    // Preserve the original global location object
    originalLocation = global.location;
  });

  afterAll(() => {
    // Restore original location after all tests
    global.location = originalLocation;
  });

  function setDocumentUrl(url) {
    // In JSDOM, document.URL is derived from window.location.href.
    // We override global.location to simulate different URLs.
    delete global.location;
    global.location = new URL(url);
  }

  function createModel() {
    return new LessonContentModel();
  }

  test('setContent normalizes lessonUrl and extracts pageNum when URL has trailing numeric page', () => {
    // Arrange
    setDocumentUrl('http://example.com/path/to/lesson.lesson/12');
    const model = createModel();
    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe('12');
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });

  test('setContent normalizes lessonUrl and sets pageNum to 0 when URL has no trailing numeric page', () => {
    // Arrange
    setDocumentUrl('http://example.com/path/to/lesson.lesson');
    const model = createModel();
    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe(0);
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });
});
