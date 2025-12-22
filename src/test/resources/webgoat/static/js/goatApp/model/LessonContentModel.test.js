// File path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// NOTE: This test assumes a Jest environment with JSDOM and that the AMD module can be required
// via a relative path. Adjust the import mechanism as appropriate for your project setup.

const LessonContentModelFactory = require('../../../../../webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel (delta tests for regex and URL handling)', () => {
  let LessonContentModel;
  let model;

  beforeAll(() => {
    // The AMD define in the source returns the extended model when invoked;
    // here we obtain the model constructor from the factory-style module export.
    // If your bundler exports differently, adapt this accordingly.
    LessonContentModel = LessonContentModelFactory;
  });

  beforeEach(() => {
    model = new LessonContentModel();
    // Stub Backbone.Model.prototype.fetch to avoid real network calls while still
    // allowing setContent behavior to be tested in isolation.
    jest.spyOn(LessonContentModel.prototype, 'fetch').mockImplementation(function (options) {
      const dfd = {
        done: (cb) => {
          cb('<html></html>');
          return dfd;
        }
      };
      return dfd;
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('setContent normalizes lessonUrl and extracts pageNum from URL with page number', () => {
    // Arrange
    const originalUrl =
      'http://localhost/WebGoat/lesson/SQLInjection.lesson/12';
    Object.defineProperty(window, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'http://localhost/WebGoat/lesson/SQLInjection.lesson'
    );
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent sets pageNum to 0 when URL has no page number suffix', () => {
    // Arrange
    const originalUrl =
      'http://localhost/WebGoat/lesson/SQLInjection.lesson';
    Object.defineProperty(window, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'http://localhost/WebGoat/lesson/SQLInjection.lesson'
    );
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent emits content:loaded event after parsing URL', () => {
    // Arrange
    const originalUrl =
      'http://localhost/WebGoat/lesson/SQLInjection.lesson/3';
    Object.defineProperty(window, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    const listener = jest.fn();
    model.on('content:loaded', listener);

    // Act
    model.setContent('<html></html>', true);

    // Assert
    expect(listener).toHaveBeenCalledTimes(1);
    const [selfArg, loadHelpsArg] = listener.mock.calls[0];
    expect(selfArg).toBe(model);
    expect(loadHelpsArg).toBe(true);
  });
});
