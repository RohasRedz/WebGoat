define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'],
    function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

    describe('LessonContentModel security-related behavior', function () {

        it('should derive lessonUrl and numeric pageNum safely for valid lesson URL', function () {
            // Arrange
            var model = new LessonContentModel();
            var originalUrl = 'http://example.com/x/y/example.lesson/123';
            var storedUrl;
            var storedPage;

            spyOn(model, 'trigger'); // avoid side effects from events

            // Simulate browser URL
            Object.defineProperty(window, 'location', {
                value: {
                    href: originalUrl
                },
                writable: true
            });
            Object.defineProperty(document, 'URL', {
                value: originalUrl,
                writable: true
            });

            // Act
            model.setContent('<html/>', true);

            // Extract values from model after setContent
            storedUrl = model.get('lessonUrl');
            storedPage = model.get('pageNum');

            // Assert
            expect(storedUrl).toBe('http://example.com/x/y/example.lesson');
            expect(storedPage).toBe('123');
        });

        it('should default pageNum to 0 for non-matching URL patterns', function () {
            // Arrange
            var model = new LessonContentModel();
            var originalUrl = 'http://example.com/other/page';
            spyOn(model, 'trigger');

            Object.defineProperty(window, 'location', {
                value: {
                    href: originalUrl
                },
                writable: true
            });
            Object.defineProperty(document, 'URL', {
                value: originalUrl,
                writable: true
            });

            // Act
            model.setContent('<html/>', true);

            // Assert
            expect(model.get('pageNum')).toBe(0);
            expect(model.get('lessonUrl')).toBe(originalUrl);
        });
    });
});
