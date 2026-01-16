/* eslint-env jest */

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

const LessonContentModel = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel – regex hardening for lessonUrl and pageNum', () => {
  test('setContent derives lessonUrl and pageNum using hardened regexes', () => {
    const originalUrl = 'http://example.com/path/to/page.lesson/123/extra?param=value';
    const previousDocumentUrl = global.document && global.document.URL;
    global.document = { URL: originalUrl };

    const model = new LessonContentModel();

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/page.lesson');
    expect(model.get('pageNum')).toBe('123');

    if (previousDocumentUrl) {
      global.document.URL = previousDocumentUrl;
    }
  });

  test('setContent sets pageNum to 0 when URL has no page number', () => {
    const originalUrl = 'http://example.com/path/to/page.lesson';
    const previousDocumentUrl = global.document && global.document.URL;
    global.document = { URL: originalUrl };

    const model = new LessonContentModel();

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/page.lesson');
    expect(model.get('pageNum')).toBe(0);

    if (previousDocumentUrl) {
      global.document.URL = previousDocumentUrl;
    }
  });
});
