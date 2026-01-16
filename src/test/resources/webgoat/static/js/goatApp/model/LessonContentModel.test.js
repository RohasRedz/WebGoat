// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {

    describe('LessonContentModel - ReDoS-safe URL handling', function () {

        it('should correctly extract pageNum and normalize lessonUrl using the new regex', function () {
            // Arrange
            var model = new LessonContentModel();
            var originalUrl = 'http://example.com/lesson/1234';
            var previousDocument = global.document;
            global.document = { URL: originalUrl };

            try {
                // Act
                model.setContent('<html/>', true);

                // Assert
                expect(model.get('lessonUrl')).toBe('http://example.com/lesson');
                expect(model.get('pageNum')).toBe('1234');
            } finally {
                // Cleanup
                global.document = previousDocument;
            }
        });

        it('should set pageNum to 0 when URL has no page number suffix', function () {
            // Arrange
            var model = new LessonContentModel();
            var originalUrl = 'http://example.com/lesson';
            var previousDocument = global.document;
            global.document = { URL: originalUrl };

            try {
                // Act
                model.setContent('<html/>', true);

                // Assert
                expect(model.get('lessonUrl')).toBe('http://example.com/lesson');
                expect(model.get('pageNum')).toBe(0);
            } finally {
                // Cleanup
                global.document = previousDocument;
            }
        });
    });
});
