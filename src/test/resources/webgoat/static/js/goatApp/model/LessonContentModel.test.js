// File path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel' // TODO: Adjust module ID to match actual AMD loader configuration if different
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  'use strict';

  // Delta tests for LessonContentModel focusing on changed regex and URL handling behavior.
  // These tests verify:
  // - setContent derives lessonUrl using the new, more specific regex.
  // - setContent extracts pageNum correctly without using backtracking-prone patterns.

  describe('LessonContentModel - delta security tests', function () {
    var originalDocumentUrl;

    beforeEach(function () {
      originalDocumentUrl = window.document.URL;
    });

    afterEach(function () {
      window.document.URL = originalDocumentUrl;
    });

    function createModel() {
      return new LessonContentModel();
    }

    it('setContent normalizes lessonUrl using anchored regex without catastrophic backtracking', function () {
      // Arrange: construct a long, adversarial URL that would stress /.lesson.*/ style regexes.
      var longPathSegment = new Array(5000).join('a');
      window.document.URL = 'https://example.org/lesson/' + longPathSegment + '.lesson/123';

      var model = createModel();

      // Act
      model.setContent('<html>dummy</html>', false);

      // Assert:
      // With the new regex /\.lesson(?:\/.*)?$/, lessonUrl should be truncated at ".lesson".
      var lessonUrl = model.get('lessonUrl');
      expect(lessonUrl).toBe('https://example.org/lesson/' + longPathSegment + '.lesson');
    });

    it('setContent extracts pageNum using match-based extraction with safe regex', function () {
      // Arrange
      window.document.URL = 'https://example.org/path/to/anything.lesson/42';
      var model = createModel();

      // Act
      model.setContent('<html>dummy</html>', false);

      // Assert
      expect(model.get('pageNum')).toBe('42');
    });

    it('setContent falls back to pageNum 0 when URL does not match pattern', function () {
      // Arrange
      window.document.URL = 'https://example.org/path/to/no-lesson-here';
      var model = createModel();

      // Act
      model.setContent('<html>dummy</html>', false);

      // Assert
      expect(model.get('pageNum')).toBe(0);
    });
  });
});
