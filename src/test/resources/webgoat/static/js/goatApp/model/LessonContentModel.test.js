// Delta_UnitTest_Agent
// Assumption: AMD module is loaded via requirejs in tests; we focus solely on the
// behavior changed by the regex and URL processing fixes.
// File under test: src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

/* eslint-env jest */

// TODO: Adjust the module path below if your RequireJS configuration differs.
define([
  'goatApp/model/LessonContentModel',
  'jquery',
  'underscore',
  'backbone'
], function (LessonContentModel, $, _, Backbone) {
  'use strict';

  describe('LessonContentModel - delta tests for regex and URL handling', function () {
    beforeEach(function () {
      // JSDOM-style global document for URL-dependent logic.
      global.document = {
        URL: 'http://localhost/WebGoat.lesson/12'
      };
    });

    test('setContent normalizes lessonUrl without expensive greedy regex', function () {
      // Arrange
      var model = new LessonContentModel();
      var content = '<html>dummy</html>';

      // Act
      model.setContent(content);

      // Assert
      // After the fix, lessonUrl should be the .lesson base, without trailing page number.
      expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat.lesson');
    });

    test('setContent extracts pageNum using streamlined regex and match()', function () {
      // Arrange
      var model = new LessonContentModel();
      var content = '<html>dummy</html>';
      global.document.URL = 'http://localhost/WebGoat.lesson/123';

      // Act
      model.setContent(content);

      // Assert
      // After the fix, pageNum is derived via document.URL.match(/\.lesson\/(\d{1,4})$/)
      // instead of repeated greedy patterns and replace() calls.
      expect(model.get('pageNum')).toBe('123');
    });

    test('setContent defaults pageNum to 0 when no page suffix is present', function () {
      // Arrange
      var model = new LessonContentModel();
      var content = '<html>dummy</html>';
      global.document.URL = 'http://localhost/WebGoat.lesson';

      // Act
      model.setContent(content);

      // Assert
      expect(model.get('pageNum')).toBe(0);
    });

    test('loadData builds urlRoot from encodeURIComponent without double-encoding', function () {
      // Arrange
      var model = new LessonContentModel();
      // Spy on Backbone.Model.prototype.fetch to avoid real network calls.
      var fetchSpy = jest.spyOn(Backbone.Model.prototype, 'fetch').mockImplementation(function () {
        return {
          done: function (cb) {
            cb('<html>dummy</html>');
            return this;
          }
        };
      });

      var options = { name: 'Some Lesson Name' };

      // Act
      model.loadData(options);

      // Assert
      // urlRoot should be encoded once and suffixed with ".lesson".
      expect(model.urlRoot).toBe(encodeURIComponent(options.name) + '.lesson');
      expect(fetchSpy).toHaveBeenCalled();

      // Cleanup
      fetchSpy.mockRestore();
    });
  });
});
