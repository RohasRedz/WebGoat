define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel' // TODO: Adjust AMD path if needed for your loader
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
    'use strict';

    /**
     * Delta tests focused on the changes to URL/regex handling in setContent():
     *  - lessonUrl computed from window.location.origin + pathname
     *  - pageNum extracted via bounded regex on pathname with numeric validation
     */
    describe('LessonContentModel delta tests', function () {

        function createModelInstance() {
            return new LessonContentModel();
        }

        function withMockLocation(url, fn) {
            var originalLocation = window.location;
            delete window.location;
            window.location = new URL(url);
            try {
                fn();
            } finally {
                window.location = originalLocation;
            }
        }

        it('sets lessonUrl based on origin and pathname and derives numeric pageNum from pathname', function () {
            var model = createModelInstance();
            var sampleUrl = 'https://example.com/lesson/1234';

            withMockLocation(sampleUrl, function () {
                model.setContent('<html></html>');

                // The fixed code uses:
                // window.location.origin + window.location.pathname.replace(/\.lesson.*/, '.lesson')
                var expectedLessonUrl = window.location.origin +
                    window.location.pathname.replace(/\.lesson.*/, '.lesson');

                expect(model.get('lessonUrl')).toBe(expectedLessonUrl);
                expect(model.get('pageNum')).toBe(1234);
            });
        });

        it('defaults pageNum to 0 when pathname does not end with a 1-4 digit number', function () {
            var model = createModelInstance();
            var sampleUrl = 'https://example.com/lesson/not-a-number';

            withMockLocation(sampleUrl, function () {
                model.setContent('<html></html>');

                expect(model.get('pageNum')).toBe(0);
            });
        });

        it('clamps pageNum to 0 when extracted digits are out of expected bounds', function () {
            var model = createModelInstance();
            // Even though regex only allows 1-4 digits, this tests the numeric validation path.
            var sampleUrl = 'https://example.com/lesson/99999';

            withMockLocation(sampleUrl, function () {
                model.setContent('<html></html>');

                // The regex /\/(\d{1,4})$/ will not match 5 digits, so pageNum should be 0
                expect(model.get('pageNum')).toBe(0);
            });
        });
    });
});
