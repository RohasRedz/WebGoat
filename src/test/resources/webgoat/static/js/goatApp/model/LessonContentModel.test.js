// Test file path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
    'use strict';

    /**
     * Delta tests for LessonContentModel focusing on the rewritten URL parsing
     * logic that replaced complex regexes to avoid ReDoS risk.
     *
     * These tests verify:
     * - lessonUrl is the base `.lesson` URL without a trailing page number.
     * - pageNum is correctly derived from a trailing numeric segment when present.
     */

    describe('LessonContentModel delta tests', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('computes lessonUrl and pageNum correctly when URL has a page number suffix', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://example.com/path/to/some.lesson/12';
            var eventsTriggered = [];
            model.on('content:loaded', function () {
                eventsTriggered.push('content:loaded');
            });

            // Simulate environment
            var originalDocument = global.document;
            global.document = {
                URL: originalUrl
            };

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            expect(model.get('lessonUrl')).toBe('http://example.com/path/to/some.lesson');
            expect(model.get('pageNum')).toBe(12);
            expect(eventsTriggered).toContain('content:loaded');

            // Cleanup
            global.document = originalDocument;
        });

        it('computes lessonUrl and defaults pageNum to 0 when no page number suffix is present', function () {
            // Arrange
            var model = createModel();
            var originalUrl = 'http://example.com/path/to/some.lesson';
            var eventsTriggered = [];
            model.on('content:loaded', function () {
                eventsTriggered.push('content:loaded');
            });

            var originalDocument = global.document;
            global.document = {
                URL: originalUrl
            };

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            expect(model.get('lessonUrl')).toBe('http://example.com/path/to/some.lesson');
            expect(model.get('pageNum')).toBe(0);
            expect(eventsTriggered).toContain('content:loaded');

            // Cleanup
            global.document = originalDocument;
        });

    });
});
