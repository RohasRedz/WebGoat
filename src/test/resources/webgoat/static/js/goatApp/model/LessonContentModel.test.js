// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

define(['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'], function (
  $,
  _,
  Backbone,
  HTMLContentModel
) {
  // NOTE: This AMD-style wrapper is only to keep import semantics similar to the production file.
  // The Jest tests themselves focus on the changed behavior (sanitization and regex handling).

  const LessonContentModel = HTMLContentModel.extend({
    // Minimal stub; actual implementation comes from the app in real runtime.
  });

  describe('LessonContentModel delta tests', () => {
    function createModel() {
      // In production, LessonContentModel is created by AMD loader; here we assume a plain Backbone model
      // with extended methods. For delta testing we only need behavior related to URL/page parsing and
      // options.name sanitization. So we re-require the original implementation.
      // TODO: Adjust if module loader differs in real test setup.
      const OriginalModel = require('../../../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
      return new OriginalModel();
    }

    test('loadData should sanitize options.name and not allow unsafe characters into urlRoot', () => {
      const model = createModel();
      const unsafeName = 'less/on\\name?with@bad#chars<script>';
      model.loadData({ name: unsafeName });

      const urlRoot = model.urlRoot || model.get('urlRoot');
      expect(urlRoot).toBeDefined();
      expect(urlRoot.endsWith('.lesson')).toBe(true);

      // urlRoot should be encoded and must not contain raw unsafe characters
      expect(urlRoot).not.toMatch(/[\/\\?<>'"<]/);

      // Encoded value should only contain allowed URL-encoded segments
      const base = urlRoot.replace(/\.lesson$/, '');
      expect(decodeURIComponent(base)).not.toMatch(/[\/\\?<>'"<]/);
    });

    test('setContent should derive lessonUrl and pageNum with efficient regex', () => {
      const model = createModel();

      // Simulate a URL with page number at the end
      const oldHref = global.window && global.window.location && global.window.location.href;
      delete global.window.location;
      global.window.location = { href: 'http://example.com/path/to/lesson.lesson/1234' };

      try {
        model.setContent('<html></html>', true);

        const lessonUrl = model.get('lessonUrl');
        const pageNum = model.get('pageNum');

        expect(lessonUrl).toBe('http://example.com/path/to/lesson.lesson');
        expect(pageNum).toBe('1234');
      } finally {
        // Restore window.location
        if (oldHref !== undefined) {
          global.window.location = { href: oldHref };
        }
      }
    });
  });
});
