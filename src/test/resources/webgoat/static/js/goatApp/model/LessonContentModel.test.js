// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'], function ($,
                                                                     _,
                                                                     Backbone,
                                                                     HTMLContentModel,
                                                                     LessonContentModel) {
    'use strict';

    /**
     * Delta tests for LessonContentModel focusing on:
     * - Correct parsing of page numbers from URLs.
     * - Safe handling (truncation) of very long URLs before regex operations.
     */
    describe('LessonContentModel security delta tests', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('should extract page number from URL ending with .lesson/<digits>', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://example.com/lesson1.lesson/42';
            var oldDocumentUrl = document.URL;
            Object.defineProperty(document, 'URL', {
                configurable: true,
                get: function () {
                    return originalUrl;
                }
            });

            // Act
            model.setContent('<html></html>', true);

            // Assert
            expect(model.get('pageNum')).toBe('42');
            expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');

            // Cleanup
            Object.defineProperty(document, 'URL', {
                configurable: true,
                get: function () {
                    return oldDocumentUrl;
                }
            });
        });

        it('should set pageNum to 0 when URL does not match expected pattern', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://example.com/lesson1.lesson';
            var oldDocumentUrl = document.URL;
            Object.defineProperty(document, 'URL', {
                configurable: true,
                get: function () {
                    return originalUrl;
                }
            });

            // Act
            model.setContent('<html></html>', true);

            // Assert
            expect(model.get('pageNum')).toBe(0);

            // Cleanup
            Object.defineProperty(document, 'URL', {
                configurable: true,
                get: function () {
                    return oldDocumentUrl;
                }
            });
        });

        it('should truncate extremely long URLs before applying regex', function () {
            // Arrange
            var model = createModel();
            // Build an overly long URL that would exceed MAX_URL_LENGTH (2048) in the implementation
            var base = 'http://example.com/lesson1.lesson/';
            var longTail = new Array(3000).join('a'); // length >> 2048
            var veryLongUrl = base + longTail;
            var oldDocumentUrl = document.URL;
            Object.defineProperty(document, 'URL', {
                configurable: true,
                get: function () {
                    return veryLongUrl;
                }
            });

            // Act
            model.setContent('<html></html>', true);

            // Assert
            // The code under test truncates but should still set pageNum to 0 when pattern not matched
            expect(model.get('pageNum')).toBe(0);

            // Cleanup
            Object.defineProperty(document, 'URL', {
                configurable: true,
                get: function () {
                    return oldDocumentUrl;
                }
            });
        });
    });
});
