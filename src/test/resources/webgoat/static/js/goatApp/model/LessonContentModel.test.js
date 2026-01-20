define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel delta tests - URL and pageNum derivation', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('should build urlRoot using encodeURIComponent on name', function () {
            var model = createModel();
            var options = { name: 'Lesson 1 & Intro' };

            model.loadData(options);

            expect(model.urlRoot).toBe(encodeURIComponent(options.name) + '.lesson');
        });

        it('should derive lessonUrl and pageNum from standard lesson URL', function () {
            var model = createModel();

            var originalLocation = window.location;
            delete window.location;
            window.location = { href: 'http://example.com/attack.lesson/12' };

            try {
                model.setContent('<html></html>', true);

                expect(model.get('lessonUrl')).toBe('http://example.com/attack.lesson');
                expect(model.get('pageNum')).toBe('12');
            } finally {
                window.location = originalLocation;
            }
        });

        it('should set pageNum to 0 when URL does not match pattern', function () {
            var model = createModel();

            var originalLocation = window.location;
            delete window.location;
            window.location = { href: 'http://example.com/attack.lesson' };

            try {
                model.setContent('<html></html>', true);

                expect(model.get('lessonUrl')).toBe('http://example.com/attack.lesson');
                expect(model.get('pageNum')).toBe(0);
            } finally {
                window.location = originalLocation;
            }
        });

    });
});
