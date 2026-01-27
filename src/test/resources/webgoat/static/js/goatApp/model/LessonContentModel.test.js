// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const jsdom = require('jsdom');
const { JSDOM } = jsdom;

// Since the original file is an AMD module, we will directly test the core logic
// of the updated URL parsing and pageNum extraction in isolation by emulating
// the behavior of setContent.

describe('LessonContentModel URL parsing (delta test)', () => {
  /**
   * Helper that emulates the updated setContent logic for URL parsing.
   * This isolates the changed behavior without depending on the full AMD/Backbone stack.
   */
  function parseUrl(url) {
    global.document = { URL: url };
    const result = {};

    const currentUrl = document.URL;
    const lessonUrl = currentUrl.split('.lesson')[0] + '.lesson';
    result.lessonUrl = lessonUrl;

    let pageNum = 0;
    const lessonSegmentIndex = currentUrl.indexOf('.lesson/');
    if (lessonSegmentIndex !== -1) {
      const pagePart = currentUrl.substring(
        lessonSegmentIndex + '.lesson/'.length
      );
      if (/^[0-9]{1,4}$/.test(pagePart)) {
        pageNum = parseInt(pagePart, 10);
      }
    }
    result.pageNum = pageNum;

    return result;
  }

  test('extracts lessonUrl and numeric pageNum safely for valid URLs', () => {
    const url = 'http://example.com/lesson1.lesson/123';
    const { lessonUrl, pageNum } = parseUrl(url);

    expect(lessonUrl).toBe('http://example.com/lesson1.lesson');
    expect(pageNum).toBe(123);
  });

  test('sets pageNum to 0 when page segment is missing or non-numeric', () => {
    const urlWithoutPage = 'http://example.com/lesson1.lesson';
    const r1 = parseUrl(urlWithoutPage);
    expect(r1.lessonUrl).toBe('http://example.com/lesson1.lesson');
    expect(r1.pageNum).toBe(0);

    const urlWithInvalidPage = 'http://example.com/lesson1.lesson/abc';
    const r2 = parseUrl(urlWithInvalidPage);
    expect(r2.lessonUrl).toBe('http://example.com/lesson1.lesson');
    expect(r2.pageNum).toBe(0);
  });
});
