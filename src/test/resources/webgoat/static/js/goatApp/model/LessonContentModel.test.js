// Derived test path (per instructions):
// src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// -> src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {
  'use strict';

  // Minimal test harness using Jasmine-style expectations, which Jest supports.
  // These tests focus only on the changed URL parsing and page number logic
  // in setContent.

  describe('LessonContentModel delta tests', function () {
    var LessonContentModel;

    beforeAll(function () {
      // Re-require the module under test using the same AMD path
      LessonContentModel = require('../../../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
    });

    function createModel() {
      // HTMLContentModel is extended; for delta testing, we only need
      // Backbone.Model behavior for set/get/trigger.
      return new LessonContentModel();
    }

    it('derives lessonUrl and pageNum correctly for URL with page number', function () {
      // Arrange
      var model = createModel();
      var originalHref = global.window && global.window.location ? global.window.location.href : 'http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson/3';

      delete global.window;
      global.window = {
        location: {
          href: 'http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson/3'
        }
      };
      global.document = { URL: global.window.location.href };

      // Act
      model.setContent('<html>test</html>', true);

      // Assert
      expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson');
      expect(model.get('pageNum')).toBe(3);

      // Restore
      global.window.location.href = originalHref;
    });

    it('falls back to pageNum 0 when URL has no page number', function () {
      // Arrange
      var model = createModel();
      delete global.window;
      global.window = {
        location: {
          href: 'http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson'
        }
      };
      global.document = { URL: global.window.location.href };

      // Act
      model.setContent('<html>test</html>', true);

      // Assert
      expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson');
      expect(model.get('pageNum')).toBe(0);
    });
  });
});
