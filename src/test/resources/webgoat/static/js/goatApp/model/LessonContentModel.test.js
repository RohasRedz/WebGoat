// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Jest tests focused on the changed behavior: safe handling of `options.name` and URL parsing regex.

define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel delta tests', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('trims, bounds, and normalizes options.name before building urlRoot', function () {
            var model = createModel();

            var options = {
                name: '  some/very\\long\\name-with-path ' +
                    new Array(200).join('x') // exceed 128 chars
            };

            model.loadData(options);

            var urlRoot = model.urlRoot;
            expect(urlRoot).toMatch(/\\.lesson$/);

            var encodedPart = urlRoot.replace(/\\.lesson$/, '');
            var decoded = decodeURIComponent(encodedPart);

            expect(decoded.length).toBeLessThanOrEqual(128);
            expect(decoded).not.toMatch(/[\\/]/);
        });

        it('extracts lessonUrl and pageNum using constrained regex without throwing for complex URLs', function () {
            var model = createModel();

            var originalUrl = window.location.href;
            delete window.location;
            // simulate complex but valid URL
            window.location = {
                href: 'http://example.com/path/to/lesson.lesson/1234?foo=bar#section'
            };

            model.setContent('<div>content</div>', true);

            expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson.lesson');
            expect(model.get('pageNum')).toBe('1234');

            window.location = { href: originalUrl };
        });

        it('defaults pageNum to 0 when URL does not match pattern', function () {
            var model = createModel();

            var originalUrl = window.location.href;
            delete window.location;
            window.location = {
                href: 'http://example.com/path/to/lesson.lesson?foo=bar'
            };

            model.setContent('<div>content</div>', true);

            expect(model.get('pageNum')).toBe(0);

            window.location = { href: originalUrl };
        });
    });
});
