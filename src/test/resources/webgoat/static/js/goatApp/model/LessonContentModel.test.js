const Backbone = require('backbone');

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;
  let instance;

  beforeAll(() => {
    // Minimal require assuming module exports the Backbone model in test environment.
    LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  beforeEach(() => {
    instance = new LessonContentModel();
  });

  test('setContent sets lessonUrl truncated at .lesson using efficient logic', () => {
    const originalUrl = 'http://example.com/path/lesson1.lesson/extra/path';
    global.document = { URL: originalUrl };

    instance.setContent('<html></html>', true);

    // After the fix, lessonUrl should still end at `.lesson`
    expect(instance.get('lessonUrl')).toBe('http://example.com/path/lesson1.lesson');
  });

  test('setContent sets numeric pageNum when URL ends in .lesson/<digits>', () => {
    const url = 'http://example.com/abc.lesson/1234';
    global.document = { URL: url };

    instance.setContent('<html></html>', true);

    expect(instance.get('pageNum')).toBe('1234');
  });

  test('setContent sets pageNum to 0 when URL has no trailing page number', () => {
    const url = 'http://example.com/abc.lesson';
    global.document = { URL: url };

    instance.setContent('<html></html>', true);

    expect(instance.get('pageNum')).toBe(0);
  });
});
