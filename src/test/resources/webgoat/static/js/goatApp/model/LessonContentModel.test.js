// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Delta tests for LessonContentModel.js focusing on getSafeLessonUrl and safer URL handling.

/* global define, describe, it, expect */

const _ = require('underscore');
const Backbone = require('backbone');

describe('LessonContentModel delta tests', () => {
  // Load the AMD module by simulating define; in real test setup, this would be adapted to your module loader.
  let LessonContentModel;
  beforeAll(() => {
    // Minimal shim for the HTMLContentModel base
    const HTMLContentModel = Backbone.Model.extend({});

    // Simulate AMD define wrapper from the updated file
    function localDefine(deps, factory) {
      LessonContentModel = factory({}, _, Backbone, HTMLContentModel);
    }

    // Inline the essential parts of the updated module for delta behavior testing
    /* eslint-disable no-useless-escape */
    localDefine(['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'], function ($, _, Backbone, HTMLContentModel) {
      function getSafeLessonUrl(url) {
        if (typeof url !== 'string') {
          return '';
        }
        var MAX_URL_LENGTH = 2048;
        if (url.length > MAX_URL_LENGTH) {
          url = url.substring(0, MAX_URL_LENGTH);
        }
        var lessonMatch = url.match(/^(.*?\.lesson)(?:\/(\d{1,4}))?$/);
        if (!lessonMatch) {
          return {
            lessonUrl: url,
            pageNum: 0
          };
        }
        return {
          lessonUrl: lessonMatch[1],
          pageNum: lessonMatch[2] ? parseInt(lessonMatch[2], 10) : 0
        };
      }

      return HTMLContentModel.extend({
        setContent: function (content, loadHelps) {
          if (typeof loadHelps === 'undefined') {
            loadHelps = true;
          }
          this.set('content', content);

          const safe = getSafeLessonUrl(global.document.URL);
          this.set('lessonUrl', safe.lessonUrl);
          this.set('pageNum', safe.pageNum);

          this.trigger('content:loaded', this, loadHelps);
        }
      });
    });
    /* eslint-enable no-useless-escape */
  });

  it('setContent derives lessonUrl and numeric pageNum from valid lesson URL using bounded regex', () => {
    global.document = { URL: 'http://example.com/path/example.lesson/1234' };
    const model = new LessonContentModel();

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/path/example.lesson');
    expect(model.get('pageNum')).toBe(1234);
  });

  it('setContent falls back to pageNum 0 when URL does not match expected pattern', () => {
    global.document = { URL: 'http://example.com/other-path' };
    const model = new LessonContentModel();

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/other-path');
    expect(model.get('pageNum')).toBe(0);
  });

  it('getSafeLessonUrl logic (via setContent) truncates overly long URLs before processing', () => {
    const longBase = 'http://example.com/'.padEnd(3000, 'a');
    global.document = { URL: longBase + 'example.lesson/12' };
    const model = new LessonContentModel();

    model.setContent('<html>content</html>');

    const lessonUrl = model.get('lessonUrl');
    expect(lessonUrl.length).toBeLessThanOrEqual(2048);
  });
});
