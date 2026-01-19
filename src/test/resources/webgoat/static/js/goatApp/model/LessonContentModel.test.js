// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
define(['jquery', 'underscore', 'backbone', 'goatApp/model/LessonContentModel'], function (
  $,
  _,
  Backbone,
  LessonContentModel
) {
  'use strict';

  /**
   * Delta tests for LessonContentModel focusing on the changed regex behavior:
   * - Ensure URL handling uses a bounded tail segment.
   * - Ensure page number extraction still works for normal URLs.
   */

  describe('LessonContentModel delta tests', function () {
    var originalUrl;

    beforeEach(function () {
      originalUrl = window.location.href;
    });

    afterEach(function () {
      window.history.replaceState(null, '', originalUrl);
    });

    function setDocumentUrl(url) {
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: new URL(url),
      });
    }

    it('extracts pageNum correctly from typical lesson URL after bounding regex', function () {
      setDocumentUrl('http://example.com/path/lessonName.lesson/42');

      var model = new LessonContentModel();
      model.setContent('<html>content</html>', true);

      expect(model.get('pageNum')).toBe('42');
    });

    it('handles extremely long URLs without catastrophic backtracking', function () {
      var longSegment = new Array(5000).join('a');
      setDocumentUrl(
        'http://example.com/' + longSegment + '/lessonName.lesson/1234'
      );

      var model = new LessonContentModel();

      var start = Date.now();
      model.setContent('<html>content</html>', true);
      var elapsed = Date.now() - start;

      expect(elapsed).toBeLessThan(1000);
      expect(model.get('pageNum')).toBe('1234');
    });
  });
});
