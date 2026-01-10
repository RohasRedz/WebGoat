// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Delta tests for LessonContentModel.js focusing on changed behavior:
// - URL root construction no longer uses _.escape(encodeURIComponent(...)) but encodeURIComponent(...) directly
// - Regex logic for pageNum should still parse valid URLs correctly

define(['jquery', 'underscore', 'backbone', 'goatApp/model/LessonContentModel'], function (
  $,
  _,
  Backbone,
  LessonContentModel
) {
  describe('LessonContentModel delta tests', () => {
    test('loadData should build urlRoot using encodeURIComponent directly', () => {
      // Arrange
      const model = new LessonContentModel();
      const options = { name: 'Lesson 1/Intro' };

      // Act
      model.loadData(options);

      // Assert
      const expected = encodeURIComponent(options.name) + '.lesson';
      expect(model.urlRoot).toBe(expected);
    });

    test('setContent should correctly derive pageNum from URL pattern', () => {
      // Arrange
      const model = new LessonContentModel();
      const originalUrl = global.document && global.document.URL;
      global.document = global.document || {};
      global.document.URL = 'http://example.com/path/lesson.lesson/42';

      // Act
      model.setContent('<html>content</html>', true);

      // Assert
      expect(model.get('pageNum')).toBe('42');

      // Cleanup
      if (originalUrl !== undefined) {
        global.document.URL = originalUrl;
      }
    });
  });
});
