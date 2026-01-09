// Test file path (derived): src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  'use strict';

  /**
   * Delta tests for LessonContentModel focusing on the safer regex logic in setContent.
   */
  describe('LessonContentModel setContent regex behavior', function () {
    it('derives lessonUrl and pageNum from URL with page number', function () {
      // Arrange
      var model = new LessonContentModel();
      var originalUrl = 'http://localhost:8080/WebGoat/start.mvc.lesson/1234';
      var oldDocumentUrl = global.document && global.document.URL;
      global.document = global.document || {};
      global.document.URL = originalUrl;

      try {
        // Act
        model.setContent('<html>dummy</html>', true);

        // Assert
        expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.mvc.lesson');
        expect(model.get('pageNum')).toBe('1234');
      } finally {
        // Cleanup
        if (oldDocumentUrl !== undefined) {
          global.document.URL = oldDocumentUrl;
        }
      }
    });

    it('derives lessonUrl and default pageNum from URL without page number', function () {
      // Arrange
      var model = new LessonContentModel();
      var originalUrl = 'http://localhost:8080/WebGoat/start.mvc.lesson';
      var oldDocumentUrl = global.document && global.document.URL;
      global.document = global.document || {};
      global.document.URL = originalUrl;

      try {
        // Act
        model.setContent('<html>dummy</html>', true);

        // Assert
        expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.mvc.lesson');
        expect(model.get('pageNum')).toBe(0);
      } finally {
        // Cleanup
        if (oldDocumentUrl !== undefined) {
          global.document.URL = oldDocumentUrl;
        }
      }
    });
  });
});
