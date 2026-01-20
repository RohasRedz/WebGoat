define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {
    'use strict';

    /**
     * Delta tests for LessonContentModel focusing on the regex/URL parsing changes:
     * - Ensures lessonUrl is derived using the safer regex without over-matching.
     * - Ensures pageNum is correctly extracted or set to 0 using the new logic.
     */

    describe('LessonContentModel URL parsing (delta tests)', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('should derive lessonUrl using constrained pattern and not include extra path segments', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://localhost:8080/WebGoat/start.lesson/1234?foo=bar';
            var previousDocumentUrl = window.document.URL;
            Object.defineProperty(window.document, 'URL', {
                configurable: true,
                writable: true,
                value: originalUrl
            });

            // Act
            model.setContent('<html></html>', false);

            // Assert
            // The updated code uses /\.lesson(?:\/.*)?$/ to derive baseLessonUrl.
            // For "start.lesson/1234?foo=bar" this should become "start.lesson".
            var lessonUrl = model.get('lessonUrl');
            expect(lessonUrl).toMatch(/\.lesson$/);
            expect(lessonUrl).toBe('http://localhost:8080/WebGoat/start.lesson');

            // Cleanup
            Object.defineProperty(window.document, 'URL', {
                configurable: true,
                writable: true,
                value: previousDocumentUrl
            });
        });

        it('should set pageNum based on trailing digits after .lesson using the safer regex', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://localhost:8080/WebGoat/start.lesson/42';
            var previousDocumentUrl = window.document.URL;
            Object.defineProperty(window.document, 'URL', {
                configurable: true,
                writable: true,
                value: originalUrl
            });

            // Act
            model.setContent('<html></html>', false);

            // Assert
            // The updated code uses /\.lesson\/(\d{1,4})$/ and match() to set pageNum.
            expect(model.get('pageNum')).toBe('42');

            // Cleanup
            Object.defineProperty(window.document, 'URL', {
                configurable: true,
                writable: true,
                value: previousDocumentUrl
            });
        });

        it('should set pageNum to 0 if URL does not match the expected pattern', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://localhost:8080/WebGoat/start.lesson/page-not-numeric';
            var previousDocumentUrl = window.document.URL;
            Object.defineProperty(window.document, 'URL', {
                configurable: true,
                writable: true,
                value: originalUrl
            });

            // Act
            model.setContent('<html></html>', false);

            // Assert
            expect(model.get('pageNum')).toBe(0);

            // Cleanup
            Object.defineProperty(window.document, 'URL', {
                configurable: true,
                writable: true,
                value: previousDocumentUrl
            });
        });
    });
});
