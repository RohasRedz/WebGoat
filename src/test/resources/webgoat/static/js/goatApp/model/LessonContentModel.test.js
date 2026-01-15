define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {
  'use strict';

  // Delta tests focus on the changed behavior in setContent and loadData:
  // - use of precompiled regex constants
  // - correct parsing of lessonUrl and pageNum from document.URL
  // - safe encoding/escaping of options.name in loadData

  describe('LessonContentModel delta tests', function () {
    let originalUrl;

    beforeEach(function () {
      originalUrl = global.document && global.document.URL;
      // Provide a default URL for the tests.
      global.document = {
        URL: 'http://example.com/SomeLesson.lesson/12'
      };
    });

    afterEach(function () {
      if (originalUrl !== undefined && global.document) {
        global.document.URL = originalUrl;
      }
    });

    it('setContent correctly derives lessonUrl and pageNum using precompiled regex', function (done) {
      // Arrange
      const model = new LessonContentModel();

      model.on('content:loaded', function (m) {
        // Assert
        expect(m.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
        expect(m.get('pageNum')).toBe('12'); // extracted capturing group

        done();
      });

      // Act
      model.setContent('<html>content</html>');
    });

    it('setContent falls back to pageNum 0 when URL does not match pattern', function (done) {
      // Arrange
      global.document.URL = 'http://example.com/Other.page';

      const model = new LessonContentModel();

      model.on('content:loaded', function (m) {
        expect(m.get('lessonUrl')).toBe('http://example.com/Other.lesson');
        expect(m.get('pageNum')).toBe(0);
        done();
      });

      // Act
      model.setContent('<html>content</html>');
    });

    it('loadData encodes and escapes options.name when building urlRoot', function () {
      // Arrange
      const model = new LessonContentModel();
      const fetchSpy = spyOn(Backbone.Model.prototype, 'fetch').and.callFake(function (options) {
        // Simulate immediate success to trigger done callbacks if any.
        if (options && typeof options.success === 'function') {
          options.success('<html>data</html>');
        }
        const dfd = $.Deferred();
        dfd.resolve('<html>data</html>');
        return dfd.promise();
      });

      const options = { name: 'Some Lesson?/&' };

      // Act
      model.loadData(options);

      // Assert
      // urlRoot should contain an encoded, escaped name ending with '.lesson'
      expect(model.urlRoot.endsWith('.lesson')).toBe(true);
      expect(model.urlRoot).not.toContain(' '); // encoded
      expect(fetchSpy).toHaveBeenCalled();
    });
  });
});
