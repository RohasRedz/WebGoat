// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  // Note: This AMD-style test assumes the same module loading environment as the application.
  // The focus is to validate the changed regex behavior in setContent.

  describe('LessonContentModel delta tests', function () {
    var originalLocation;

    beforeEach(function () {
      originalLocation = window.location;
      delete window.location;
      window.location = {
        href: 'http://localhost',
        toString: function () {
          return this.href;
        }
      };
      Object.defineProperty(document, 'URL', {
        configurable: true,
        get: function () {
          return window.location.href;
        }
      });
    });

    afterEach(function () {
      window.location = originalLocation;
    });

    function createModelInstance() {
      return new LessonContentModel();
    }

    it('setContent computes lessonUrl and pageNum correctly with safer regex when URL has page number', function () {
      window.location.href = 'http://example.com/path/to/lesson.lesson/1234';

      var model = createModelInstance();

      model.setContent('<div>content</div>');

      expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson.lesson');
      expect(model.get('pageNum')).toBe('1234');
    });

    it('setContent falls back to pageNum 0 when URL has no trailing page number', function () {
      window.location.href = 'http://example.com/path/to/lesson.lesson';

      var model = createModelInstance();

      model.setContent('<div>content</div>');

      expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson.lesson');
      expect(model.get('pageNum')).toBe(0);
    });
  });
});
