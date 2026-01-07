define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  'webgoat/static/js/goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  'use strict';

  describe('LessonContentModel regex parsing (delta tests)', function () {
    var originalUrl;

    beforeEach(function () {
      originalUrl = global.document && global.document.URL;
    });

    afterEach(function () {
      if (global.document) {
        global.document.URL = originalUrl;
      }
    });

    function createModel() {
      return new LessonContentModel();
    }

    it('should set lessonUrl without page number for URLs without page segment', function () {
      var model = createModel();
      global.document = global.document || {};
      global.document.URL = 'http://example.com/WebGoat/attack.lesson';

      model.setContent('<html>ignored</html>');

      expect(model.get('lessonUrl')).toBe('http://example.com/WebGoat/attack.lesson');
      expect(model.get('pageNum')).toBe(0);
    });

    it('should correctly extract lessonUrl and pageNum for URLs with page number', function () {
      var model = createModel();
      global.document = global.document || {};
      global.document.URL = 'http://example.com/WebGoat/attack.lesson/3';

      model.setContent('<html>ignored</html>');

      expect(model.get('lessonUrl')).toBe('http://example.com/WebGoat/attack.lesson');
      expect(model.get('pageNum')).toBe('3');
    });

    it('should fall back gracefully for malformed URLs', function () {
      var model = createModel();
      global.document = global.document || {};
      global.document.URL = 'not-a-normal-url';

      model.setContent('<html>ignored</html>');

      expect(model.get('lessonUrl')).toBe('not-a-normal-url');
      expect(model.get('pageNum')).toBe(0);
    });
  });
});
