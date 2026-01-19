// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

  /**
   * Delta tests focus on the changed regex-based URL handling:
   * - lessonUrl should be normalized to the base .lesson URL
   * - pageNum should be extracted from trailing /digits or default to 0
   */
  describe('LessonContentModel delta tests', function () {

    function createModel() {
      return new LessonContentModel();
    }

    it('sets lessonUrl to base .lesson URL and extracts pageNum when present', function () {
      // Arrange
      var model = createModel();
      var originalUrl = 'http://example.com/lesson/Path.lesson/123';
      var oldDocumentUrl = global.document && global.document.URL;
      global.document = global.document || {};
      global.document.URL = originalUrl;

      // Act
      model.setContent('<html></html>', true);

      // Assert
      expect(model.get('lessonUrl')).toBe('http://example.com/lesson/Path.lesson');
      expect(model.get('pageNum')).toBe('123');

      // Cleanup
      if (oldDocumentUrl !== undefined) {
        global.document.URL = oldDocumentUrl;
      }
    });

    it('defaults pageNum to 0 when no trailing page segment exists', function () {
      var model = createModel();
      var originalUrl = 'http://example.com/lesson/Path.lesson';
      var oldDocumentUrl = global.document && global.document.URL;
      global.document = global.document || {};
      global.document.URL = originalUrl;

      model.setContent('<html></html>', true);

      expect(model.get('lessonUrl')).toBe('http://example.com/lesson/Path.lesson');
      expect(model.get('pageNum')).toBe(0);

      if (oldDocumentUrl !== undefined) {
        global.document.URL = oldDocumentUrl;
      }
    });
  });
});
