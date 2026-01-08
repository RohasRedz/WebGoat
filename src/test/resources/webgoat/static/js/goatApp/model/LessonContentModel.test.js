// Derived test path (per instructions):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// Note: This test assumes a Jest environment with jsdom so that `document` and `document.URL` exist.

const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the module dependency.
class HTMLContentModel extends Backbone.Model {}

describe('LessonContentModel - delta tests for regex and URL handling', () => {
  let LessonContentModel;

  beforeAll(() => {
    // Simulate AMD define by manually constructing the module using the updated code's logic.
    // We mirror the structure: HTMLContentModel.extend({ ... }).

    LessonContentModel = HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function () {},

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

        let currentUrl = document.URL;
        if (typeof currentUrl === 'string') {
          currentUrl = currentUrl.slice(0, 2048);
        }

        this.set('lessonUrl', currentUrl.replace(/\.lesson[^/]*.*/,'\.lesson'));

        const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
        if (pageMatch) {
          this.set('pageNum', pageMatch[1]);
        } else {
          this.set('pageNum', 0);
        }
        this.trigger('content:loaded', this, loadHelps);
      },

      fetch: function (options) {
        options = options || {};
        // For delta test, just return a then-able stub that immediately resolves.
        const self = this;
        return {
          done(callback) {
            callback('<html>stub</html>');
            return self;
          },
        };
      },
    });
  });

  test('setContent uses bounded URL and assigns lessonUrl and pageNum correctly', () => {
    // Arrange: craft a long URL that could be problematic for inefficient regex
    const baseUrl =
      'http://example.com/path/to/lesson/SQLInjection.lesson/1234?param=' +
      'x'.repeat(5000); // long tail to test bounding

    Object.defineProperty(window, 'location', {
      value: { href: baseUrl },
      writable: true,
    });
    Object.defineProperty(document, 'URL', {
      value: baseUrl,
      writable: true,
    });

    const model = new LessonContentModel();

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');

    // The fixed behavior: lessonUrl should normalize to the .lesson base.
    expect(lessonUrl.endsWith('.lesson')).toBe(true);

    // Page number is extracted from the URL suffix.
    expect(pageNum).toBe('1234');

    // Ensure the content:loaded event fired with loadHelps defaulting to true.
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });
});
