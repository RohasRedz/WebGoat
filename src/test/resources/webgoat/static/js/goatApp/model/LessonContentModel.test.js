/* eslint-env jest */

const path = require('path');

// NOTE: This test assumes the AMD module is exposed in a test bundle environment.
// If not, these tests should be adapted to the actual module loading mechanism.
// TODO: Adjust the import/require to match the real bundling setup if necessary.
describe('LessonContentModel delta tests', () => {
  let LessonContentModel;
  let Backbone;
  let modelInstance;

  beforeAll(() => {
    // In a typical WebGoat setup, LessonContentModel is an AMD module.
    // For Jest, we assume it has been bundled to a CommonJS module for tests.
    // eslint-disable-next-line global-require
    Backbone = require('backbone');
    // eslint-disable-next-line global-require
    LessonContentModel = require('../../../webgoat/static/js/goatApp/model/LessonContentModel');
  });

  beforeEach(() => {
    modelInstance = new LessonContentModel();
  });

  test('setContent computes lessonUrl without using complex regex', () => {
    // Arrange: simulate a URL with a .lesson path and page number
    const originalUrl = 'https://example.org/WebGoat/lesson/Intro.lesson/12';
    const oldLocation = global.window && global.window.location;

    delete global.window;
    global.window = {
      location: {
        origin: 'https://example.org',
      },
    };
    global.document = {
      URL: originalUrl,
    };

    // Act
    modelInstance.setContent('<html/>', true);

    // Assert
    const lessonUrl = modelInstance.get('lessonUrl');
    expect(lessonUrl).toBe('/WebGoat/lesson/Intro.lesson');

    const pageNum = modelInstance.get('pageNum');
    expect(pageNum).toBe(12);

    // Cleanup
    if (oldLocation) {
      global.window.location = oldLocation;
    }
  });

  test('setContent falls back to pageNum 0 when URL has no page segment', () => {
    // Arrange
    const originalUrl = 'https://example.org/WebGoat/lesson/Intro.lesson';
    delete global.window;
    global.window = {
      location: {
        origin: 'https://example.org',
      },
    };
    global.document = {
      URL: originalUrl,
    };

    // Act
    modelInstance.setContent('<html/>', true);

    // Assert
    const lessonUrl = modelInstance.get('lessonUrl');
    expect(lessonUrl).toBe('/WebGoat/lesson/Intro.lesson');

    const pageNum = modelInstance.get('pageNum');
    expect(pageNum).toBe(0);
  });
});
