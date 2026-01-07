const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the dependency
const HTMLContentModel = Backbone.Model.extend({});

// AMD-style define wrapper simulation
jest.mock('goatApp/model/HTMLContentModel', () => HTMLContentModel, { virtual: true });

describe('LessonContentModel - regex based URL parsing', () => {
  let LessonContentModel;

  beforeAll(() => {
    // Simulate AMD define by requiring the module after mocks
    LessonContentModel = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  beforeEach(() => {
    global.document = { URL: '' };
  });

  test('setContent extracts pageNum and lessonUrl when URL ends with .lesson/<digits>', () => {
    document.URL = 'http://localhost/WebGoat/lesson/SqlInjection.lesson/12';
    const model = new LessonContentModel();

    const loadedSpy = jest.fn();
    model.on('content:loaded', loadedSpy);

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SqlInjection.lesson');
    expect(model.get('pageNum')).toBe('12');
    expect(loadedSpy).toHaveBeenCalled();
  });

  test('setContent sets pageNum to 0 when URL does not end with .lesson/<digits>', () => {
    document.URL = 'http://localhost/WebGoat/lesson/SqlInjection.lesson';
    const model = new LessonContentModel();

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SqlInjection.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
