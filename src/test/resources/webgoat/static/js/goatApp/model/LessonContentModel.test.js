define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {

    describe('LessonContentModel.setContent (delta tests for regex changes)', function () {
        var originalUrl;

        beforeEach(function () {
            originalUrl = global.document && global.document.URL;
            global.document = {
                URL: 'http://localhost/WebGoat/lesson/Intro.lesson/1'
            };
        });

        afterEach(function () {
            if (originalUrl !== undefined && global.document) {
                global.document.URL = originalUrl;
            }
        });

        function createModel() {
            return new LessonContentModel();
        }

        it('computes lessonUrl without trailing page when URL has .lesson/page', function () {
            var model = createModel();
            global.document.URL = 'http://localhost/WebGoat/Intro.lesson/3';

            model.setContent('<html>dummy</html>', true);

            expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/Intro.lesson');
            expect(model.get('pageNum')).toBe('3');
        });

        it('defaults pageNum to 0 when URL does not match the page pattern', function () {
            var model = createModel();
            global.document.URL = 'http://localhost/WebGoat/Intro.lesson';

            model.setContent('<html>dummy</html>', true);

            expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/Intro.lesson');
            expect(model.get('pageNum')).toBe(0);
        });

        it('handles URLs with query parameters safely and still parses page number', function () {
            var model = createModel();
            global.document.URL = 'http://localhost/WebGoat/Intro.lesson/42?foo=bar';

            model.setContent('<html>dummy</html>', true);

            expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/Intro.lesson');
            expect(model.get('pageNum')).toBe('42');
        });

        it('does not degrade performance for very long, adversarial URLs', function () {
            var model = createModel();
            var longPath = new Array(5000).join('a/');
            global.document.URL = 'http://localhost/' + longPath + 'Intro.lesson/7';

            var start = Date.now();
            model.setContent('<html>dummy</html>', true);
            var elapsed = Date.now() - start;

            expect(elapsed).toBeLessThan(1000);
            expect(model.get('lessonUrl')).toMatch(/Intro\.lesson$/);
            expect(model.get('pageNum')).toBe('7');
        });
    });
});
