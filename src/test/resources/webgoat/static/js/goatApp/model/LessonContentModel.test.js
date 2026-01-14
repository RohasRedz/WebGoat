const _ = require('underscore');
const Backbone = require('backbone');

// Assuming HTMLContentModel is resolvable from the same path as in production code.
// If the actual path differs in the test setup, adjust the require accordingly.
const HTMLContentModel = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/HTMLContentModel');
const LessonContentModel = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel');

/**
 * Delta tests for LessonContentModel focusing on the regex changes:
 * - verify lessonUrl normalization uses the new safer pattern
 * - verify pageNum extraction behaves correctly for valid and invalid URLs
 */
describe('LessonContentModel regex behavior (delta tests)', () => {
  let model;
  let originalLocation;

  beforeEach(() => {
    originalLocation = global.document && global.document.URL;
    global.document = { URL: '' };
    model = new LessonContentModel();
  });

  afterEach(() => {
    if (originalLocation !== undefined) {
      global.document.URL = originalLocation;
    }
  });

  test('setContent normalizes lessonUrl using new regex without catastrophic backtracking', () => {
    global.document.URL = 'http://example.com/my.lesson/extra/path/123';

    model.setContent('<html/>', true);

    expect(model.get('lessonUrl')).toBe('http://example.com/my.lesson');
  });

  test('setContent extracts numeric pageNum from valid lesson URL', () => {
    global.document.URL = 'http://example.com/my.lesson/42';

    model.setContent('<html/>', true);

    expect(model.get('pageNum')).toBe('42');
  });

  test('setContent sets pageNum to 0 when URL does not match expected pattern', () => {
    global.document.URL = 'http://example.com/another.page';

    model.setContent('<html/>', true);

    expect(model.get('pageNum')).toBe(0);
  });
});
