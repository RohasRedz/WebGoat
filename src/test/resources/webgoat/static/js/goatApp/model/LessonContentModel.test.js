jest.mock('backbone', () => {
  const Backbone = {
    Model: class {
      constructor(attrs) {
        this.attributes = attrs || {};
      }
      set(key, value) {
        this.attributes[key] = value;
      }
      get(key) {
        return this.attributes[key];
      }
      trigger() {
        // no-op for this delta test
      }
      fetch() {
        // This will be overridden by the model under test;
        // no behavior needed here for regex-focused tests.
      }
    }
  };
  Backbone.Model.prototype.fetch = function () {
    return { done: (cb) => cb('<html/>') };
  };
  return Backbone;
});

jest.mock('underscore', () => ({
  extend: (target, source) => Object.assign(target, source),
}));

jest.mock('jquery', () => ({}));

describe('LessonContentModel regex delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    jest.resetModules();
    // Load the AMD-style module via its built artifact; we simulate a simple define wrapper.
    // Since we cannot easily execute the original AMD define in Jest without a loader,
    // we assume the build exposes the model as a CommonJS module at the given path.
    LessonContentModel = require('../../../../../webgoat/static/js/goatApp/model/LessonContentModel.js'); // TODO: Adjust path if necessary

    global.document = {
      URL: 'http://localhost/WebGoat/lesson.lesson'
    };
  });

  test('setContent normalizes lessonUrl with new bounded regex', () => {
    // Arrange
    const model = new LessonContentModel();

    // Act
    model.setContent('<html/>', true);

    // Assert: lessonUrl ends with ".lesson" and does not include page suffix
    const lessonUrl = model.get('lessonUrl');
    expect(lessonUrl.endsWith('.lesson')).toBe(true);
    expect(lessonUrl).toBe('http://localhost/WebGoat/lesson.lesson');
  });

  test('setContent extracts pageNum using new precompiled regex when present', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://localhost/WebGoat/lesson.lesson/1234';

    // Act
    model.setContent('<html/>', true);

    // Assert: pageNum matches last path segment of 1–4 digits
    expect(model.get('pageNum')).toBe('1234');
  });

  test('setContent sets pageNum to 0 when URL does not match the lesson page pattern', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://localhost/WebGoat/lesson.lesson/not-a-page';

    // Act
    model.setContent('<html/>', true);

    // Assert: non-matching URL results in pageNum 0
    expect(model.get('pageNum')).toBe(0);
  });
});
