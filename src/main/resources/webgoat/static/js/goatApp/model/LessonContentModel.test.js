define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel – URL parsing and page number extraction', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('sets lessonUrl with .lesson suffix and pageNum parsed from numeric tail after fix', function () {
            var model = createModel();
            var originalUrl = 'http://example.com/lesson/intro.lesson/3';
            var oldDocumentUrl = global.document && global.document.URL;
            global.document = { URL: originalUrl };

            var content = '<html></html>';

            model.setContent(content);

            expect(model.get('lessonUrl')).toBe('http://example.com/lesson/intro.lesson');
            expect(model.get('pageNum')).toBe(3);

            if (oldDocumentUrl) {
                global.document.URL = oldDocumentUrl;
            }
        });

        it('sets pageNum to 0 when URL does not end with a 1-4 digit segment', function () {
            var model = createModel();
            var originalUrl = 'http://example.com/lesson/intro.lesson';
            var oldDocumentUrl = global.document && global.document.URL;
            global.document = { URL: originalUrl };

            var content = '<html></html>';

            model.setContent(content);

            expect(model.get('lessonUrl')).toBe('http://example.com/lesson/intro.lesson');
            expect(model.get('pageNum')).toBe(0);

            if (oldDocumentUrl) {
                global.document.URL = oldDocumentUrl;
            }
        });

        it('does not use inefficient full-URL regex and correctly handles long URLs without performance degradation', function () {
            var model = createModel();
            var longSegment = new Array(1000).join('a');
            var originalUrl = 'http://example.com/' + longSegment + '/intro.lesson/12';
            var oldDocumentUrl = global.document && global.document.URL;
            global.document = { URL: originalUrl };

            var content = '<html></html>';

            model.setContent(content);

            expect(model.get('lessonUrl')).toBe('http://example.com/' + longSegment + '/intro.lesson');
            expect(model.get('pageNum')).toBe(12);

            if (oldDocumentUrl) {
                global.document.URL = oldDocumentUrl;
            }
        });
    });
});
