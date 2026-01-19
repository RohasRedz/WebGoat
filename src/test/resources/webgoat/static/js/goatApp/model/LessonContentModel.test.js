// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
/* eslint-env jest */

define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel' // TODO: adjust AMD module path if needed
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel delta tests - safer regex behavior', () => {
        let model;

        beforeEach(() => {
            model = new LessonContentModel();
        });

        test('setContent extracts page number using simplified regex and ignores complex prefixes', () => {
            // Arrange: URL with long prefix that could previously trigger expensive backtracking
            const originalUrl =
                'http://example.com/some/really/long/path/with/many/segments/and/query?foo=bar.lesson/1234';
            const previousDocument = global.document;
            global.document = {
                URL: originalUrl
            };

            try {
                // Act
                model.setContent('<html>content</html>', true);

                // Assert:
                // The simplified regex still correctly extracts the 4-digit page number at the end.
                expect(model.get('pageNum')).toBe('1234');

                // And the lessonUrl still ends with ".lesson"
                expect(model.get('lessonUrl').endsWith('.lesson')).toBe(true);
            } finally {
                global.document = previousDocument;
            }
        });

        test('setContent sets pageNum to 0 when no page number is present', () => {
            const previousDocument = global.document;
            global.document = {
                URL: 'http://example.com/lesson/intro.lesson'
            };

            try {
                model.setContent('<html>content</html>', true);
                expect(model.get('pageNum')).toBe(0);
            } finally {
                global.document = previousDocument;
            }
        });
    });
});
