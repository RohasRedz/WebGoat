define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel',
    'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
    // Delta tests focus on the updated URL parsing and regex behavior in setContent

    describe('LessonContentModel - delta tests for URL parsing', function () {

        function createModel() {
            return new LessonContentModel();
        }

        it('sets lessonUrl and pageNum correctly for URL with page number (secure regex behavior)', function () {
            // Arrange
            var originalUrl = 'http://localhost/WebGoat.lesson/12';
            var model = createModel();
            var setSpy = jest.spyOn(model, 'set');
            var triggerSpy = jest.spyOn(model, 'trigger').mockImplementation(function () {});

            var originalDocumentUrl = global.document && global.document.URL;
            global.document = global.document || {};
            global.document.URL = originalUrl;

            // Act
            model.setContent('<html>dummy</html>', true);

            // Assert
            // New behavior: use bounded regex with match; verify resulting values
            expect(setSpy).toHaveBeenCalledWith('content', '<html>dummy</html>');
            expect(setSpy).toHaveBeenCalledWith('lessonUrl', 'http://localhost/WebGoat.lesson');
            expect(setSpy).toHaveBeenCalledWith('pageNum', '12');

            expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);

            // Cleanup
            setSpy.mockRestore();
            triggerSpy.mockRestore();
            if (originalDocumentUrl !== undefined) {
                global.document.URL = originalDocumentUrl;
            }
        });

        it('defaults pageNum to 0 when URL does not end with page number', function () {
            // Arrange
            var originalUrl = 'http://localhost/WebGoat.lesson';
            var model = createModel();
            var setSpy = jest.spyOn(model, 'set');
            var triggerSpy = jest.spyOn(model, 'trigger').mockImplementation(function () {});

            var originalDocumentUrl = global.document && global.document.URL;
            global.document = global.document || {};
            global.document.URL = originalUrl;

            // Act
            model.setContent('<html>dummy</html>');

            // Assert
            expect(setSpy).toHaveBeenCalledWith('lessonUrl', 'http://localhost/WebGoat.lesson');
            expect(setSpy).toHaveBeenCalledWith('pageNum', 0);

            // Cleanup
            setSpy.mockRestore();
            triggerSpy.mockRestore();
            if (originalDocumentUrl !== undefined) {
                global.document.URL = originalDocumentUrl;
            }
        });
    });
});
