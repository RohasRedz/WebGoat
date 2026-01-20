// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define(['jquery', 'underscore', 'backbone'], function ($, _, Backbone) {
  // We assume AMD-style environment for this delta test; in a real setup,
  // this test would import the module under test via the project's loader configuration.

  describe('LessonContentModel delta tests for regex and URL handling', function () {
    let LessonContentModel;

    beforeAll(function (done) {
      // Load the updated module; in a real test environment this would be wired via RequireJS config.
      require(['webgoat/static/js/goatApp/model/LessonContentModel'], function (Model) {
        LessonContentModel = Model;
        done();
      });
    });

    it('should normalize lessonUrl without catastrophic regex and correctly derive pageNum', function () {
      // Arrange
      const model = new LessonContentModel();
      const originalUrl = 'http://example.com/attack.lesson/123';
      const originalDocument = global.document || {};
      global.document = { URL: originalUrl };

      // Act
      model.setContent('<html></html>', true);

      // Assert
      expect(model.get('lessonUrl')).toBe('http://example.com/attack.lesson');
      expect(model.get('pageNum')).toBe('123');

      // Cleanup
      global.document = originalDocument;
    });

    it('should set pageNum to 0 when URL does not contain a page suffix', function () {
      // Arrange
      const model = new LessonContentModel();
      const originalUrl = 'http://example.com/attack.lesson';
      const originalDocument = global.document || {};
      global.document = { URL: originalUrl };

      // Act
      model.setContent('<html></html>', true);

      // Assert
      expect(model.get('lessonUrl')).toBe('http://example.com/attack.lesson');
      expect(model.get('pageNum')).toBe(0);

      // Cleanup
      global.document = originalDocument;
    });
  });
});
