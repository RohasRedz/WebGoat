// TODO: Adjust the module path if the actual AMD loader resolution differs from this assumption.
define([
  'jquery',
  'underscore',
  'backbone',
  'goatApp/model/HTMLContentModel',
  // We load the model under test as an AMD module.
  'goatApp/model/LessonContentModel'
], function ($, _, Backbone, HTMLContentModel, LessonContentModel) {
  'use strict';

  /**
   * Delta tests for LessonContentModel focusing ONLY on the changed URL/regex behavior
   * that fixed the "Inefficient Regular Expression Complexity" vulnerability.
   *
   * The fix:
   *  - Replaced greedy regexes on document.URL with anchored, constrained patterns.
   *  - Introduced getSafeDocumentUrl() and safer setContent() logic.
   *
   * These tests assert:
   *  - lessonUrl is derived correctly from window.location.href using the new regex.
   *  - pageNum is parsed correctly from URLs that match the `.lesson/<digits>` pattern.
   *  - pageNum defaults to 0 when the pattern does not match.
   */

  describe('LessonContentModel (delta tests for regex and URL handling)', function () {
    let originalLocation;

    beforeAll(function () {
      originalLocation = window.location;
      // Jest-like approach for jsdom; ensure we have a configurable location.
      delete window.location;
      window.location = { href: 'http://example.com' };
    });

    afterAll(function () {
      window.location = originalLocation;
    });

    function createModel() {
      // Instantiate the Backbone model under test
      return new LessonContentModel();
    }

    it('should derive lessonUrl by replacing .lesson suffix using safe anchored regex', function () {
      const model = createModel();
      window.location.href = 'https://test.local/lesson/intro.lesson/extra/path';

      model.setContent('<html>dummy</html>');

      const lessonUrl = model.get('lessonUrl');
      // The new logic uses /\\.lesson(?:\\/.*)?$/ to produce a clean .lesson URL.
      expect(lessonUrl).toBe('https://test.local/lesson/intro.lesson');
    });

    it('should set pageNum from URLs ending with .lesson/<1-4 digits>', function () {
      const model = createModel();
      window.location.href = 'https://test.local/lesson/intro.lesson/123';

      model.setContent('<html>dummy</html>');

      const pageNum = model.get('pageNum');
      expect(pageNum).toBe(123);
    });

    it('should default pageNum to 0 when URL does not match .lesson/<digits> pattern', function () {
      const model = createModel();
      window.location.href = 'https://test.local/lesson/intro.lesson';

      model.setContent('<html>dummy</html>');

      const pageNum = model.get('pageNum');
      expect(pageNum).toBe(0);
    });
  });
});
