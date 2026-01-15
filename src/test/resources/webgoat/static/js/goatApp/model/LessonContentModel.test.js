// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// Delta tests for LessonContentModel.js focusing on the input-validation and URL-handling changes.
// - Ensure options.name is sanitized and constrained.
// - Ensure a safe default is used when name is missing/invalid.
// - Ensure pageNum and lessonUrl are derived as expected from document.URL.

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  'use strict';

  describe('LessonContentModel delta tests', function () {
    var originalUrl;

    beforeEach(function () {
      originalUrl = global.document && global.document.URL;
      // Provide a minimal DOM-like object if not present.
      if (typeof document === 'undefined') {
        global.document = { URL: 'http://example.com/Intro.lesson/1' };
      } else {
        document.URL = 'http://example.com/Intro.lesson/1';
      }
    });

    afterEach(function () {
      if (typeof originalUrl !== 'undefined') {
        document.URL = originalUrl;
      }
    });

    it('sanitizes options.name and falls back to safe default', function () {
      var model = new LessonContentModel();

      // Name contains characters that should be stripped by the new regex.
      var options = { name: 'Intro../..//\\?<script>' };
      spyOn(Backbone.Model.prototype, 'fetch').and.callFake(function (opts) {
        // URL root should be built from sanitized "Intro" only.
        expect(model.urlRoot).toBe(encodeURIComponent('Intro') + '.lesson');
        // Ensure options still passed through.
        expect(opts).toBeDefined();
        return {
          done: function () {
            return this;
          }
        };
      });

      model.loadData(options);
    });

    it('uses default index when name is missing or becomes empty after sanitization', function () {
      var model = new LessonContentModel();

      spyOn(Backbone.Model.prototype, 'fetch').and.callFake(function () {
        expect(model.urlRoot).toBe(encodeURIComponent('index') + '.lesson');
        return {
          done: function (cb) {
            // Simulate backend returning HTML payload; this will exercise setContent.
            cb('<h1>Lesson</h1>');
            return this;
          }
        };
      });

      model.loadData({ name: '!!!@@@' }); // should sanitize to empty and use "index"
      expect(model.get('lessonUrl')).toBe('http://example.com/Intro.lesson');
      expect(model.get('pageNum')).toBe('1');
    });

    it('sets pageNum to 0 when URL does not contain page segment', function () {
      var model = new LessonContentModel();
      document.URL = 'http://example.com/Intro.lesson';

      spyOn(Backbone.Model.prototype, 'fetch').and.callFake(function () {
        return {
          done: function (cb) {
            cb('<h1>Lesson</h1>');
            return this;
          }
        };
      });

      model.loadData({ name: 'Intro' });

      expect(model.get('lessonUrl')).toBe('http://example.com/Intro.lesson');
      expect(model.get('pageNum')).toBe(0);
    });
  });
});
