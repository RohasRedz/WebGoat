jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({});
});

const Backbone = require('backbone');

describe('LessonContentModel.js regex hardening delta tests', () => {
  let LessonContentModel;
  let originalLocation;

  beforeEach(() => {
    jest.resetModules();
    originalLocation = global.location;

    global.window = global.window || {};
    global.document = global.document || {};

    LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  afterEach(() => {
    global.location = originalLocation;
  });

  function createModelWithUrl(url) {
    global.document.URL = url;
    const model = new LessonContentModel();
    return model;
  }

  test('setContent normalizes lessonUrl by stripping trailing segments after .lesson', () => {
    const model = createModelWithUrl('http://example.com/path/to/lesson/1234');

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson');
  });

  test('setContent extracts pageNum when URL ends with .lesson/<digits>', () => {
    const model = createModelWithUrl('http://example.com/course.lesson/42');

    model.setContent('<html>content</html>', true);

    expect(model.get('pageNum')).toBe('42');
  });

  test('setContent sets pageNum to 0 when URL has no trailing numeric page', () => {
    const model = createModelWithUrl('http://example.com/course.lesson');

    model.setContent('<html>content</html>', true);

    expect(model.get('pageNum')).toBe(0);
  });
});
