// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

/**
 * Delta tests for LessonContentModel focusing only on the changed regex-based URL parsing.
 * These tests verify:
 *  - lessonUrl normalization now safely trims only the optional numeric page suffix.
 *  - pageNum extraction uses the updated, more specific regex and matching logic.
 */

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {
  'use strict';

  function createModel() {
    // The production code returns HTMLContentModel.extend({...}),
    // so we instantiate it as a Backbone model.
    return new LessonContentModel();
  }

  describe('LessonContentModel delta tests for regex changes', function () {
    var originalLocation;

    beforeEach(function () {
      // Save original global location/document.URL
      originalLocation = global.location;
      // JSDOM-style fake location
      global.location = { href: 'http://example.com' };
      // Document shim
      global.document = {
        URL: ''
      };
    });

    afterEach(function () {
      global.location = originalLocation;
      delete global.document;
    });

    it('normalizes lessonUrl by removing optional page number suffix only', function (done) {
      // Arrange
      var model = createModel();
      var url =
        'http://localhost:8080/WebGoat/start.mvc#lesson/SqlInjection.lesson/12';
      global.document.URL = url;

      // We cannot easily hit the real fetch() network, so we call setContent directly.
      // Old behavior used a broad /\\.lesson.*/ pattern; new behavior uses
      // /\.lesson(?:\/[0-9]{1,4})?$/ to avoid catastrophic backtracking.
      model.on('content:loaded', function () {
        // Assert
        var lessonUrl = model.get('lessonUrl');
        expect(lessonUrl).toBe(
          'http://localhost:8080/WebGoat/start.mvc#lesson/SqlInjection.lesson'
        );
        done();
      });

      // Act
      model.setContent('<html>dummy</html>');
    });

    it('extracts pageNum using the safer match-based regex', function (done) {
      // Arrange: URL ending with .lesson/<pageNum>
      var model = createModel();
      global.document.URL =
        'http://localhost:8080/WebGoat/start.mvc#lesson/SqlInjection.lesson/27';

      model.on('content:loaded', function () {
        var pageNum = model.get('pageNum');
        expect(pageNum).toBe('27'); // captured as the first group
        done();
      });

      // Act
      model.setContent('<html>dummy</html>');
    });

    it('sets pageNum to 0 when no numeric suffix is present', function (done) {
      // Arrange: URL without /<pageNum> suffix
      var model = createModel();
      global.document.URL =
        'http://localhost:8080/WebGoat/start.mvc#lesson/SqlInjection.lesson';

      model.on('content:loaded', function () {
        var pageNum = model.get('pageNum');
        expect(pageNum).toBe(0);
        done();
      });

      // Act
      model.setContent('<html>dummy</html>');
    });
  });
});
