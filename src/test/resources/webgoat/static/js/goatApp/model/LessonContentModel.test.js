// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

/**
 * Delta tests for LessonContentModel focusing on the change from broad, potentially
 * inefficient regular expressions on document.URL to structured URL parsing and
 * bounded numeric checks for the page number.
 *
 * These tests verify:
 *  - lessonUrl is normalized to the base ".lesson" path without using the old
 *    regex-based replacement behavior.
 *  - pageNum is derived only from a 1–4 digit final path segment.
 */

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

  describe('LessonContentModel delta behavior', function () {
    let originalDocumentUrl;

    beforeEach(function () {
      originalDocumentUrl = window.document.URL;
    });

    afterEach(function () {
      window.document.URL = originalDocumentUrl;
    });

    it('normalizes lessonUrl to base ".lesson" path and parses numeric pageNum', function () {
      // Arrange
      // Simulate a URL with a .lesson and a numeric page segment
      window.document.URL = 'https://example.org/app/path/lesson-name.lesson/12';

      const model = new LessonContentModel();

      // Spy on trigger to ensure content:loaded is still emitted
      const triggerSpy = jest.spyOn(model, 'trigger');

      // Act
      model.setContent('<div>content</div>', true);

      // Assert
      const lessonUrl = model.get('lessonUrl');
      const pageNum = model.get('pageNum');

      expect(lessonUrl).toBe('https://example.org/app/path/lesson-name.lesson');
      expect(pageNum).toBe(12);
      expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
    });

    it('sets pageNum to 0 when last segment is not 1-4 digit number', function () {
      // Arrange
      window.document.URL = 'https://example.org/app/path/lesson-name.lesson/not-a-number';

      const model = new LessonContentModel();

      // Act
      model.setContent('<div>content</div>', false);

      // Assert
      const lessonUrl = model.get('lessonUrl');
      const pageNum = model.get('pageNum');

      expect(lessonUrl).toBe('https://example.org/app/path/lesson-name.lesson');
      expect(pageNum).toBe(0);
    });
  });
});
