define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/LessonContentModel'
], function ($, _, Backbone, LessonContentModel) {
  describe('LessonContentModel delta tests', function () {
    var model;

    beforeEach(function () {
      model = new LessonContentModel();
    });

    it('should bound options.name length and set urlRoot safely', function () {
      var longName = Array(300).join('a');
      model.loadData({ name: longName });
      expect(model.urlRoot.indexOf(encodeURIComponent(longName.substring(0, 256)))).toBe(0);
      expect(model.urlRoot.slice(-7)).toBe('.lesson');
    });

    it('should derive lessonUrl and pageNum from document.URL using precompiled regex', function () {
      var originalUrl = window.document.URL;
      window.document.URL = 'http://example.com/lesson1.lesson/42';

      model.setContent('<div>content</div>');

      expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
      expect(model.get('pageNum')).toBe(42);

      window.document.URL = originalUrl;
    });

    it('should default pageNum to 0 when URL does not contain a page segment', function () {
      var originalUrl = window.document.URL;
      window.document.URL = 'http://example.com/lesson2.lesson';

      model.setContent('<div>content</div>');

      expect(model.get('pageNum')).toBe(0);

      window.document.URL = originalUrl;
    });
  });
});
