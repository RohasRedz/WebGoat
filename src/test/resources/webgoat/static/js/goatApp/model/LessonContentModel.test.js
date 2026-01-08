// Test file path (derived from src/main/resources/...):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define([
  'jquery',
  'underscore',
  'backbone',
  'webgoat/static/js/goatApp/model/LessonContentModel',
], function ($, _, Backbone, LessonContentModel) {
  'use strict';

  /**
   * Delta tests for LessonContentModel focusing on the new URL validation logic
   * in the sync method:
   *  - Accepts safe URLs (relative, within length limit, allowed chars)
   *  - Rejects unsafe URLs (too long or invalid pattern)
   */

  describe('LessonContentModel sync URL validation', function () {
    var model;

    beforeEach(function () {
      model = new LessonContentModel();
      spyOn(Backbone, 'sync').and.callFake(function (method, m, options) {
        // Simulate a successful sync; we only care about invocation side effects.
        return { method: method, url: options && options.url };
      });
    });

    it('allows safe relative URLs and delegates to Backbone.sync', function () {
      var options = { url: '/lessons/valid-path_1.json' };

      var result = model.sync('read', model, options);

      expect(Backbone.sync).toHaveBeenCalled();
      var callArgs = Backbone.sync.calls.mostRecent().args;
      expect(callArgs[0]).toBe('read');
      expect(callArgs[2].url).toBe('/lessons/valid-path_1.json');
    });

    it('rejects URLs that are too long', function () {
      // Construct an overly long relative URL
      var longPath = '/a' + new Array(2100).join('a') + '.json';
      var options = { url: longPath };

      expect(function () {
        model.sync('read', model, options);
      }).toThrowError('Invalid or unsafe URL provided to LessonContentModel.sync');

      expect(Backbone.sync).not.toHaveBeenCalled();
    });

    it('rejects URLs that do not start with / or ./', function () {
      var options = { url: 'http://example.com/bad' };

      expect(function () {
        model.sync('read', model, options);
      }).toThrowError('Invalid or unsafe URL provided to LessonContentModel.sync');

      expect(Backbone.sync).not.toHaveBeenCalled();
    });

    it('rejects URLs containing disallowed characters', function () {
      var options = { url: '/bad?param=1' };

      expect(function () {
        model.sync('read', model, options);
      }).toThrowError('Invalid or unsafe URL provided to LessonContentModel.sync');

      expect(Backbone.sync).not.toHaveBeenCalled();
    });
  });
});
