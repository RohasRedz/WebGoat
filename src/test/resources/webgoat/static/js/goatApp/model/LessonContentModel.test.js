// Assumed logical Jest test location for WebGoat JS resources:
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the AMD dependency used in LessonContentModel.js
class HTMLContentModel extends Backbone.Model {}

// Load the module under test using a simple AMD-like shim.
// In the real project, RequireJS or a similar loader would be used.
describe('LessonContentModel - delta tests for URL regex handling', () => {
    let LessonContentModel;

    beforeAll(() => {
        // Simulate the AMD define used by LessonContentModel.js
        global.define = function (deps, factory) {
            LessonContentModel = factory($, _, Backbone, HTMLContentModel);
        };
        // Require the updated LessonContentModel implementation
        // NOTE: path mirrors resolved main path with /main/ -> /test/ replacement convention.
        require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
        delete global.define;
    });

    test('setContent should derive lessonUrl by stripping anything after .lesson', () => {
        // Arrange
        const model = new LessonContentModel();
        const originalUrl = 'http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson/3';
        delete global.document;
        global.document = { URL: originalUrl };

        // Act
        model.setContent('<html>content</html>', false);

        // Assert
        const lessonUrl = model.get('lessonUrl');
        expect(lessonUrl).toBe('http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson');
    });

    test('setContent should correctly extract numeric pageNum when URL ends with .lesson/<digits>', () => {
        // Arrange
        const model = new LessonContentModel();
        const originalUrl = 'http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson/42';
        delete global.document;
        global.document = { URL: originalUrl };

        // Act
        model.setContent('<html>content</html>');

        // Assert
        const pageNum = model.get('pageNum');
        expect(pageNum).toBe('42');
    });

    test('setContent should set pageNum to 0 when URL does not match expected pattern', () => {
        // Arrange
        const model = new LessonContentModel();
        const originalUrl = 'http://localhost/WebGoat/lesson/SqlInjectionAdvanced.lesson?page=5';
        delete global.document;
        global.document = { URL: originalUrl };

        // Act
        model.setContent('<html>content</html>');

        // Assert
        const pageNum = model.get('pageNum');
        expect(pageNum).toBe(0);
    });
});
