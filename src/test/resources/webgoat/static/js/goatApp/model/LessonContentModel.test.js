// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// Delta tests for LessonContentModel.js focusing on:
// - Safer regex behavior and page number extraction
// - Sanitization of options.name when constructing urlRoot

define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
    'use strict';

    describe('LessonContentModel delta tests', function () {
        let model;

        beforeEach(function () {
            model = new LessonContentModel();
        });

        it('setContent should derive lessonUrl without catastrophic regex and extract numeric pageNum', function () {
            // Arrange
            var originalUrl = 'http://example.com/lesson/SomeLesson.lesson/1234';
            spyOn(document, 'URL', 'get').and.returnValue(originalUrl);

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            expect(model.get('lessonUrl')).toBe('http://example.com/lesson/SomeLesson.lesson');
            expect(model.get('pageNum')).toBe('1234');
        });

        it('setContent should default pageNum to 0 when URL has no page segment', function () {
            // Arrange
            var originalUrl = 'http://example.com/lesson/SomeLesson.lesson';
            spyOn(document, 'URL', 'get').and.returnValue(originalUrl);

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            expect(model.get('pageNum')).toBe(0);
        });

        it('loadData should sanitize options.name before using it in urlRoot', function () {
            // Arrange
            // Name containing characters that should be stripped by the new allowlist
            var unsafeName = 'less/on\\name?<script>';

            spyOn(model, 'fetch').and.callFake(function () {
                return {
                    done: function (cb) {
                        cb('<html>dummy</html>');
                    }
                };
            });

            // Act
            model.loadData({ name: unsafeName });

            // Assert
            var urlRoot = model.urlRoot;
            // Expectation: only word chars, dot, dash and underscore remain
            expect(urlRoot).toMatch(/^[A-Za-z0-9_.%-]+\.lesson$/);
            expect(urlRoot).not.toContain('<');
            expect(urlRoot).not.toContain('>');
            expect(urlRoot).not.toContain('/');
            expect(urlRoot).not.toContain('\\');
        });
    });
});
