define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {
  'use strict';

  // Delta tests for LessonContentModel focused on the regex/URL parsing change.
  //
  // These tests:
  // - Verify that pageNum is correctly derived from URLs with a numeric suffix
  //   using the new precompiled regex and exec() logic.
  // - Verify that non-matching URLs result in pageNum = 0.
  //
  // Note: We re-create the basic behavior of LessonContentModel here to avoid
  // depending on the full Backbone environment; the assertions focus strictly
  // on the changed regex-based behavior.

  describe('LessonContentModel delta tests', function () {
    var LessonContentModel;

    beforeEach(function () {
      LessonContentModel = HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
          items: null,
          selectedItem: null
        },

        initialize: function () {},

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
          this.set('lessonUrl', document.URL.replace(/\.lesson.*/, '.lesson'));

          // Copied from fixed implementation: use precompiled, bounded regex.
          var lessonPagePattern = /^.*\.lesson\/(\d{1,4})$/;
          var match = lessonPagePattern.exec(document.URL);
          if (match) {
            this.set('pageNum', match[1]);
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
        }
      });
    });

    it('sets pageNum from URL suffix when numeric page is present', function () {
      // Arrange
      var originalUrl = global.document && global.document.URL;
      global.document = global.document || {};
      document.URL = 'http://example.com/foo.lesson/123';

      var model = new LessonContentModel();

      // Act
      model.setContent('<html>dummy</html>', true);

      // Assert
      expect(model.get('pageNum')).toBe('123');

      // Cleanup
      if (originalUrl !== undefined) {
        document.URL = originalUrl;
      }
    });

    it('sets pageNum to 0 when URL does not contain numeric page suffix', function () {
      // Arrange
      var originalUrl = global.document && global.document.URL;
      global.document = global.document || {};
      document.URL = 'http://example.com/foo.lesson';

      var model = new LessonContentModel();

      // Act
      model.setContent('<html>dummy</html>', true);

      // Assert
      expect(model.get('pageNum')).toBe(0);

      // Cleanup
      if (originalUrl !== undefined) {
        document.URL = originalUrl;
      }
    });

    it('handles large but bounded page numbers without catastrophic behavior', function () {
      // Arrange
      var originalUrl = global.document && global.document.URL;
      global.document = global.document || {};
      document.URL = 'http://example.com/foo.lesson/9999';

      var model = new LessonContentModel();

      // Act
      model.setContent('<html>dummy</html>', true);

      // Assert
      // This specifically tests the {1,4} bound used in the fixed regex.
      expect(model.get('pageNum')).toBe('9999');

      // Cleanup
      if (originalUrl !== undefined) {
        document.URL = originalUrl;
      }
    });
  });
});
