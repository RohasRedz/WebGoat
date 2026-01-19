define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel delta tests for URL regex changes', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('sets lessonUrl and pageNum correctly when URL has page number suffix', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://example.com/attack.lesson/12';
            var oldDocumentUrl = global.document && global.document.URL;
            global.document = global.document || {};
            global.document.URL = originalUrl;

            try {
                // Act
                model.setContent('<html>dummy</html>', true);

                // Assert
                expect(model.get('lessonUrl')).toBe('http://example.com/attack.lesson');
                expect(model.get('pageNum')).toBe('12');
            } finally {
                // Cleanup
                if (oldDocumentUrl !== undefined) {
                    global.document.URL = oldDocumentUrl;
                }
            }
        });

        it('sets pageNum to 0 when URL has no page number suffix', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://example.com/attack.lesson';
            var oldDocumentUrl = global.document && global.document.URL;
            global.document = global.document || {};
            global.document.URL = originalUrl;

            try {
                // Act
                model.setContent('<html>dummy</html>', true);

                // Assert
                expect(model.get('lessonUrl')).toBe('http://example.com/attack.lesson');
                expect(model.get('pageNum')).toBe(0);
            } finally {
                if (oldDocumentUrl !== undefined) {
                    global.document.URL = oldDocumentUrl;
                }
            }
        });
    });
});
