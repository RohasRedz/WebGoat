// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  'use strict';

  /**
   * Delta tests for LessonContentModel.js focusing on the updated, more efficient
   * regular expressions used in setContent().
   *
   * We verify that:
   * - lessonUrl is still derived correctly from document.URL using the new regex.
   * - pageNum is still extracted correctly with the new bounded pattern.
   * - No regression occurs for URLs with and without page numbers.
   */

  describe('LessonContentModel delta tests', function () {
    let originalUrl;

    beforeEach(function () {
      // Preserve original document.URL behavior if present
      originalUrl = window.location.href;
    });

    afterEach(function () {
      // Restore original URL using history API where possible
      if (window.history && window.history.replaceState) {
        window.history.replaceState(null, '', originalUrl);
      }
    });

    /**
     * Helper to simulate URL changes in tests without reloading the page.
     */
    function setDocumentUrl(url) {
      if (window.history && window.history.replaceState) {
        window.history.replaceState(null, '', url);
      } else {
        // Fallback for environments without History API in tests
        Object.defineProperty(window, 'location', {
          value: { href: url },
          writable: true
        });
      }
    }

    it('setContent derives lessonUrl and pageNum correctly for URL without page number', function () {
      // Arrange
      const url = 'https://example.com/Path/To/MyLesson.lesson';
      setDocumentUrl(url);

      const model = new LessonContentModel();
      const setSpy = spyOn(model, 'set').and.callThrough();
      const triggerSpy = spyOn(model, 'trigger').and.callThrough();

      // Act
      model.setContent('<html>content</html>');

      // Assert
      // Verify that content is set
      expect(setSpy).toHaveBeenCalledWith('content', '<html>content</html>');

      // Verify lessonUrl: should remain ".lesson" suffix using the new, safer regex
      const lessonUrlCall = setSpy.calls.all().find(function (c) {
        return c.args[0] === 'lessonUrl';
      });
      expect(lessonUrlCall).toBeDefined();
      expect(lessonUrlCall.args[1]).toBe('https://example.com/Path/To/MyLesson.lesson');

      // Verify pageNum: without "/<digits>" at the end, it should default to 0
      const pageNumCall = setSpy.calls.all().find(function (c) {
        return c.args[0] === 'pageNum';
      });
      expect(pageNumCall).toBeDefined();
      expect(pageNumCall.args[1]).toBe(0);

      // Ensure the event is still triggered
      expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
    });

    it('setContent derives lessonUrl and pageNum correctly for URL with page number', function () {
      // Arrange
      const url = 'https://example.com/Path/To/MyLesson.lesson/1234';
      setDocumentUrl(url);

      const model = new LessonContentModel();
      const setSpy = spyOn(model, 'set').and.callThrough();
      const triggerSpy = spyOn(model, 'trigger').and.callThrough();

      // Act
      model.setContent('<html>content</html>');

      // Assert
      const lessonUrlCall = setSpy.calls.all().find(function (c) {
        return c.args[0] === 'lessonUrl';
      });
      expect(lessonUrlCall).toBeDefined();
      expect(lessonUrlCall.args[1]).toBe('https://example.com/Path/To/MyLesson.lesson');

      const pageNumCall = setSpy.calls.all().find(function (c) {
        return c.args[0] === 'pageNum';
      });
      expect(pageNumCall).toBeDefined();
      expect(pageNumCall.args[1]).toBe('1234');

      expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
    });

    it('setContent falls back to pageNum 0 when URL does not end with .lesson/<digits>', function () {
      // Arrange
      const url = 'https://example.com/Path/To/MyLesson.lesson/some/other/path';
      setDocumentUrl(url);

      const model = new LessonContentModel();
      const setSpy = spyOn(model, 'set').and.callThrough();

      // Act
      model.setContent('<html>content</html>');

      // Assert
      const pageNumCall = setSpy.calls.all().find(function (c) {
        return c.args[0] === 'pageNum';
      });
      expect(pageNumCall).toBeDefined();
      expect(pageNumCall.args[1]).toBe(0);
    });
  });
});
