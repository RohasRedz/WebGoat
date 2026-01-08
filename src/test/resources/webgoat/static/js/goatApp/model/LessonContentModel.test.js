// File path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
/* eslint-env jest */
define(['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel', 'webgoat/static/js/goatApp/model/LessonContentModel'],
  function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel URL parsing', function () {
      var model;

      beforeEach(function () {
        model = new LessonContentModel();
      });

      function setDocumentUrl(url) {
        // JSDOM-compatible override
        Object.defineProperty(document, 'URL', {
          value: url,
          writable: true,
          configurable: true
        });
      }

      test('sets lessonUrl and pageNum when URL has page suffix', function () {
        // Arrange
        var url = 'http://example.com/WebGoat/lesson/SomeLesson.lesson/12';
        setDocumentUrl(url);

        // Act
        model.setContent('<html>content</html>', true);

        // Assert
        expect(model.get('lessonUrl')).toBe('http://example.com/WebGoat/lesson/SomeLesson.lesson');
        expect(model.get('pageNum')).toBe('12');
      });

      test('sets lessonUrl and default pageNum when URL has no page suffix', function () {
        // Arrange
        var url = 'http://example.com/WebGoat/lesson/SomeLesson.lesson';
        setDocumentUrl(url);

        // Act
        model.setContent('<html>content</html>', true);

        // Assert
        expect(model.get('lessonUrl')).toBe('http://example.com/WebGoat/lesson/SomeLesson.lesson');
        expect(model.get('pageNum')).toBe(0);
      });
    });
  });
