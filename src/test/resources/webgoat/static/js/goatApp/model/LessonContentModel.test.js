// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
    'use strict';

    // Delta tests using a simple Jasmine/Jest-like style via AMD; if this project
    // uses Jest directly, this module can be adapted to ES modules.
    // The focus is on the changed regex & URL handling behavior.

    describe('LessonContentModel delta tests', function () {
        var originalUrl;

        beforeEach(function () {
            originalUrl = window.document.URL;
        });

        afterEach(function () {
            window.document.URL = originalUrl;
        });

        it('should derive lessonUrl from URL with page number using anchored regex', function () {
            // Arrange
            window.document.URL = 'http://localhost/WebGoat/lesson/SqlInjection.lesson/3';
            var model = new LessonContentModel();

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SqlInjection.lesson');
            expect(model.get('pageNum')).toBe('3');
        });

        it('should set pageNum to 0 when URL has no page number', function () {
            // Arrange
            window.document.URL = 'http://localhost/WebGoat/lesson/SqlInjection.lesson';
            var model = new LessonContentModel();

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SqlInjection.lesson');
            expect(model.get('pageNum')).toBe(0);
        });
    });
});
