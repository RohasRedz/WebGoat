// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModelTest.js

// Delta tests for LessonContentModel focusing on the updated URL parsing and page number extraction.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the AMD dependency used in the updated file.
// In the real project this should import the actual module if available.
class HTMLContentModel extends Backbone.Model {}

describe('LessonContentModel URL parsing (delta tests)', () => {
    let LessonContentModel;

    beforeAll(() => {
        // Simulate AMD define for the updated module under test
        jest.isolateModules(() => {
            // Mock the AMD define used in the original file
            global.define = (deps, factory) => {
                LessonContentModel = factory($, _, Backbone, HTMLContentModel);
            };
            // Require the updated module to trigger define
            require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
        });
    });

    test('setContent normalizes lessonUrl and defaults pageNum when no page segment', () => {
        const model = new LessonContentModel();

        // Simulate a URL without page segment
        const originalUrl = 'http://localhost/WebGoat/attack.lesson';
        const originalLocation = global.document ? global.document.location : undefined;

        // Provide a minimal document mock
        global.document = {
            URL: originalUrl,
            location: { href: originalUrl }
        };

        const contentLoadedSpy = jest.fn();
        model.on('content:loaded', contentLoadedSpy);

        model.setContent('<html>content</html>');

        expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/attack.lesson');
        expect(model.get('pageNum')).toBe(0);
        expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);

        // Restore document if it previously existed
        if (originalLocation) {
            global.document = { location: originalLocation };
        }
    });

    test('setContent extracts pageNum from URL with .lesson/<page>', () => {
        const model = new LessonContentModel();

        const originalUrl = 'http://localhost/WebGoat/attack.lesson/42';
        const originalLocation = global.document ? global.document.location : undefined;

        global.document = {
            URL: originalUrl,
            location: { href: originalUrl }
        };

        model.setContent('<html>content</html>');

        expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/attack.lesson');
        expect(model.get('pageNum')).toBe('42');

        if (originalLocation) {
            global.document = { location: originalLocation };
        }
    });

    test('setContent handles complex suffixes after .lesson gracefully', () => {
        const model = new LessonContentModel();

        const originalUrl = 'http://localhost/WebGoat/attack.lesson/something/123';
        const originalLocation = global.document ? global.document.location : undefined;

        global.document = {
            URL: originalUrl,
            location: { href: originalUrl }
        };

        model.setContent('<html>content</html>');

        // The fixed regex should still reduce to the .lesson base
        expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/attack.lesson');
        // Page number does not match the strict pattern at the end, so 0 is expected
        expect(model.get('pageNum')).toBe(0);

        if (originalLocation) {
            global.document = { location: originalLocation };
        }
    });
});
