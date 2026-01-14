define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {
  'use strict';

  // Delta tests cover only the updated URL/regex handling behavior in setContent.

  describe('LessonContentModel URL and page parsing (delta tests)', function () {
    let model;
    let originalUrl;

    beforeEach(function () {
      // Save and override global document.URL for each test
      originalUrl = global.document && global.document.URL;
      global.document = global.document || {};
      model = new LessonContentModel();
    });

    afterEach(function () {
      if (originalUrl !== undefined) {
        global.document.URL = originalUrl;
      }
    });

    it('computes lessonUrl and pageNum correctly for a standard lesson page URL', function () {
      global.document.URL = 'http://localhost/WebGoat.lesson/3';

      model.setContent('<html>...</html>');

      expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat.lesson');
      expect(model.get('pageNum')).toBe('3');
    });

    it('defaults pageNum to 0 when URL has no trailing page number', function () {
      global.document.URL = 'http://localhost/WebGoat.lesson';

      model.setContent('<html>...</html>');

      expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat.lesson');
      expect(model.get('pageNum')).toBe(0);
    });

    it('handles long but benign URLs efficiently without throwing', function () {
      const longSegment = 'a'.repeat(5000);
      global.document.URL = `http://example.com/WebGoat.lesson/${longSegment}`;

      // The main assertion is that setContent() returns quickly and does not throw
      expect(function () {
        model.setContent('<html>...</html>');
      }).not.toThrow();

      // Because there is no numeric page at the end, pageNum should be 0
      expect(model.get('pageNum')).toBe(0);
      // lessonUrl should still end in '.lesson'
      expect(model.get('lessonUrl')).toMatch(/\.lesson$/);
    });
  });
});
