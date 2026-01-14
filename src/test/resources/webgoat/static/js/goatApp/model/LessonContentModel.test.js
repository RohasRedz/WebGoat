define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($,
             _,
             Backbone,
             HTMLContentModel) {

    // NOTE: This test file assumes a test runner capable of loading AMD modules.
    // It focuses only on the changed regex behavior in setContent.

    describe('LessonContentModel - delta tests', function () {
        var LessonContentModel;

        beforeAll(function (done) {
            // Dynamically require the model under test if AMD loader is available
            require(['webgoat/static/js/goatApp/model/LessonContentModel'], function (Model) {
                LessonContentModel = Model;
                done();
            });
        });

        it('should normalize lessonUrl using the bounded regex without affecting valid URLs', function () {
            var model = new LessonContentModel();

            var originalUrl = 'http://example.com/lesson/intro.lesson/12';
            var oldDocument = global.document;
            global.document = { URL: originalUrl };

            try {
                model.setContent('<html></html>', true);
                var lessonUrl = model.get('lessonUrl');

                expect(lessonUrl).toBe('http://example.com/lesson/intro.lesson');
            } finally {
                global.document = oldDocument;
            }
        });

        it('should set pageNum based on existing behavior when URL ends with digits', function () {
            var model = new LessonContentModel();

            var urlWithPage = 'http://example.com/lesson/intro.lesson/42';
            var oldDocument = global.document;
            global.document = { URL: urlWithPage };

            try {
                model.setContent('<html></html>', true);
                var pageNum = model.get('pageNum');

                expect(pageNum).toBe('42');
            } finally {
                global.document = oldDocument;
            }
        });
    });
});
