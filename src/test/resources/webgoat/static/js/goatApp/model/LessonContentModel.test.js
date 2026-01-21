// Test file path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'],
    function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

        // NOTE: These delta tests focus on the changed URL parsing logic in setContent,
        // ensuring that the new implementation preserves expected behavior while avoiding
        // inefficient regular expressions.

        describe('LessonContentModel URL parsing (delta tests)', function () {
            var model;

            beforeEach(function () {
                model = new LessonContentModel();
            });

            function withDocumentUrl(url, fn) {
                var original = global.document;
                global.document = { URL: url };
                try {
                    fn();
                } finally {
                    global.document = original;
                }
            }

            it('sets lessonUrl to .lesson base URL and numeric pageNum when URL ends with .lesson/<digits>', function () {
                withDocumentUrl('http://example.com/lesson1.lesson/12', function () {
                    // Act
                    model.setContent('<html></html>', true);

                    // Assert
                    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
                    expect(model.get('pageNum')).toBe('12');
                });
            });

            it('sets pageNum to 0 when trailing segment after .lesson is non-numeric', function () {
                withDocumentUrl('http://example.com/lesson1.lesson/not-a-number', function () {
                    model.setContent('<html></html>', true);

                    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
                    expect(model.get('pageNum')).toBe(0);
                });
            });

            it('falls back to full URL and pageNum 0 when .lesson is not present', function () {
                withDocumentUrl('http://example.com/other', function () {
                    model.setContent('<html></html>', true);

                    expect(model.get('lessonUrl')).toBe('http://example.com/other');
                    expect(model.get('pageNum')).toBe(0);
                });
            });
        });
    });
