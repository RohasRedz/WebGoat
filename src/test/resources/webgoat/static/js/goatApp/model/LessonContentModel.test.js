// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const path = require('path');

// The module under test is AMD-style. In real setup, this would be loaded via RequireJS or similar.
// For this delta test we focus on verifying the regex logic extracted from the updated implementation.
describe('LessonContentModel URL parsing (delta test)', () => {
  // Recreate the core regex behavior from the updated file for targeted testing
  const LESSON_URL_RE = /\.lesson(?:$|[?#])/;
  const PAGE_NUM_RE = /\.lesson\/(\d{1,4})$/;

  function deriveLessonUrl(url) {
    return String(url || '').replace(LESSON_URL_RE, '.lesson');
  }

  function derivePageNum(url) {
    const currentUrl = String(url || '');
    if (PAGE_NUM_RE.test(currentUrl)) {
      return currentUrl.replace(PAGE_NUM_RE, '$1');
    }
    return 0;
  }

  test('deriveLessonUrl normalizes URLs with query string and hash using bounded regex', () => {
    const url = 'http://example.com/lesson1.lesson?page=2#section';
    const normalized = deriveLessonUrl(url);

    expect(normalized).toBe('http://example.com/lesson1.lesson');
  });

  test('derivePageNum extracts page number when URL matches strict pattern', () => {
    const url = 'http://example.com/lesson1.lesson/1234';
    const pageNum = derivePageNum(url);

    expect(pageNum).toBe('1234');
  });

  test('derivePageNum returns 0 when URL does not match bounded pattern', () => {
    const url = 'http://example.com/lesson1.lesson/not-a-number';
    const pageNum = derivePageNum(url);

    expect(pageNum).toBe(0);
  });
});
