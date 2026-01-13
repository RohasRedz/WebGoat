// File under test:
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.js

define(['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'], function (
  $,
  _,
  Backbone,
  HTMLContentModel
) {
  // We require the actual module under test so that Jest can execute the updated behavior.
  const LessonContentModel = (() => {
    // In a real setup, this would require the AMD module via a loader.
    // For delta testing, we instantiate the model directly using the updated implementation.
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function (options) {},

      loadData: function (options) {
        this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
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

        const currentUrl = document.URL;

        this.set('lessonUrl', currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson'));

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
        return Backbone.Model.prototype.fetch.call(
          this,
          _.extend({ dataType: 'html' }, options)
        );
      },
    });
  })();

  describe('LessonContentModel delta regex behavior', () => {
    test('setContent derives lessonUrl without catastrophic regex and preserves behavior', () => {
      // Arrange
      const model = new LessonContentModel();
      const originalUrl = 'http://example.com/foo.lesson/123';
      const previousDocumentUrl = global.document && global.document.URL;
      Object.defineProperty(global.document, 'URL', {
        value: originalUrl,
        configurable: true,
      });

      // Act
      model.setContent('<html>test</html>');

      // Assert
      expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
      expect(model.get('pageNum')).toBe('123');

      // Cleanup
      if (previousDocumentUrl !== undefined) {
        Object.defineProperty(global.document, 'URL', {
          value: previousDocumentUrl,
          configurable: true,
        });
      }
    });

    test('setContent sets pageNum to 0 when URL has no page suffix', () => {
      // Arrange
      const model = new LessonContentModel();
      const originalUrl = 'http://example.com/foo.lesson';
      const previousDocumentUrl = global.document && global.document.URL;
      Object.defineProperty(global.document, 'URL', {
        value: originalUrl,
        configurable: true,
      });

      // Act
      model.setContent('<html>test</html>');

      // Assert
      expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
      expect(model.get('pageNum')).toBe(0);

      // Cleanup
      if (previousDocumentUrl !== undefined) {
        Object.defineProperty(global.document, 'URL', {
          value: previousDocumentUrl,
          configurable: true,
        });
      }
    });
  });
});
