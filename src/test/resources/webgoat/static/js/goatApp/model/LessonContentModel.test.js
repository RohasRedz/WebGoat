// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Delta tests for LessonContentModel.js focusing on the safer regex URL parsing behavior.

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

  describe('LessonContentModel - delta tests for safe regex URL parsing', function () {
    let model;

    beforeEach(function () {
      model = new LessonContentModel();
    });

    it('should normalize lessonUrl using the new regex without catastrophic patterns', function () {
      // Arrange: simulate a URL containing .lesson with additional segments
      const originalUrl = 'http://example.com/path/to/foo.lesson/123?param=1';
      const originalReplace = document.URL;
      Object.defineProperty(document, 'URL', {
        value: originalUrl,
        configurable: true
      });

      // Act
      model.setContent('<html>content</html>', false);

      // Assert: lessonUrl should be normalized to end with ".lesson"
      const lessonUrl = model.get('lessonUrl');
      expect(lessonUrl.endsWith('.lesson')).toBe(true);

      // Cleanup
      Object.defineProperty(document, 'URL', {
        value: originalReplace,
        configurable: true
      });
    });

    it('should extract pageNum correctly using the updated regex', function () {
      const originalUrl = 'http://example.com/lesson.foo.lesson/42';
      const originalReplace = document.URL;
      Object.defineProperty(document, 'URL', {
        value: originalUrl,
        configurable: true
      });

      model.setContent('<html>content</html>', false);

      const pageNum = model.get('pageNum');
      expect(pageNum).toBe('42');

      Object.defineProperty(document, 'URL', {
        value: originalReplace,
        configurable: true
      });
    });

    it('should set pageNum to 0 when URL does not match the expected pattern', function () {
      const originalUrl = 'http://example.com/other/path';
      const originalReplace = document.URL;
      Object.defineProperty(document, 'URL', {
        value: originalUrl,
        configurable: true
      });

      model.setContent('<html>content</html>', false);

      const pageNum = model.get('pageNum');
      expect(pageNum).toBe(0);

      Object.defineProperty(document, 'URL', {
        value: originalReplace,
        configurable: true
      });
    });
  });
});
