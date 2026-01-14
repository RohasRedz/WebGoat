/* Delta tests for LessonContentModel focusing on the updated URL parsing logic
 * that replaces complex regex usage with the URL API and bounded parsing.
 */

define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {

  describe('LessonContentModel URL parsing (delta tests)', function () {
    let originalUrl;

    beforeEach(function () {
      originalUrl = global.document && global.document.URL;
      global.document = { URL: 'http://localhost:8080/WebGoat/lesson/1234' };
    });

    afterEach(function () {
      if (originalUrl !== undefined) {
        global.document.URL = originalUrl;
      }
    });

    it('sets lessonUrl and pageNum correctly using URL-based parsing', function () {
      var model = new LessonContentModel();

      // Spy on trigger to inspect arguments without requiring full Backbone infrastructure
      spyOn(model, 'trigger');

      model.setContent('<html>content</html>', true);

      var lessonUrl = model.get('lessonUrl');
      var pageNum = model.get('pageNum');

      expect(lessonUrl).toBe('http://localhost:8080/WebGoat/lesson');
      expect(pageNum).toBe(1234);
      expect(model.trigger).toHaveBeenCalledWith('content:loaded', model, true);
    });

    it('defaults pageNum to 0 when URL has no numeric suffix', function () {
      global.document.URL = 'http://localhost:8080/WebGoat/lesson';

      var model = new LessonContentModel();
      spyOn(model, 'trigger');

      model.setContent('<html>content</html>', false);

      var lessonUrl = model.get('lessonUrl');
      var pageNum = model.get('pageNum');

      expect(lessonUrl).toBe('http://localhost:8080/WebGoat/lesson');
      expect(pageNum).toBe(0);
      expect(model.trigger).toHaveBeenCalledWith('content:loaded', model, false);
    });
  });
});
