// Assuming Jest test environment and AMD-compatible loading are configured in the project.
// Test file path (derived from source): src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {
  describe('LessonContentModel URL normalization (delta tests)', function () {
    let model;
    const originalUrl = global.document ? document.URL : 'http://example.com';

    beforeEach(function () {
      // Minimal document mock to control URL behavior
      global.document = {
        URL: 'http://webgoat.local/SomeLesson.lesson'
      };
      model = new LessonContentModel();
    });

    afterEach(function () {
      // Restore default document if needed
      global.document = { URL: originalUrl };
    });

    it('normalizes URLs ending with .lesson to .lesson without trailing parts', function () {
      document.URL = 'http://webgoat.local/SomeLesson.lesson';
      model.setContent('<html></html>');

      expect(model.get('lessonUrl')).toBe('http://webgoat.local/SomeLesson.lesson');
      expect(model.get('pageNum')).toBe(0);
    });

    it('normalizes URLs ending with .lesson/<digits> to base .lesson and extracts pageNum', function () {
      document.URL = 'http://webgoat.local/SomeLesson.lesson/12';
      model.setContent('<html></html>');

      expect(model.get('lessonUrl')).toBe('http://webgoat.local/SomeLesson.lesson');
      expect(model.get('pageNum')).toBe('12');
    });

    it('sets pageNum to 0 when URL does not end with .lesson/<digits>', function () {
      document.URL = 'http://webgoat.local/SomeLesson.lesson/foo';
      model.setContent('<html></html>');

      expect(model.get('lessonUrl')).toBe('http://webgoat.local/SomeLesson.lesson');
      expect(model.get('pageNum')).toBe(0);
    });
  });
});
