/**
 * @file src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
 *
 * Delta Jest tests for LessonContentModel.js focusing on the URL parsing and
 * regex behavior that was adjusted to address the inefficient regular expression
 * complexity finding. These tests ensure the regex-based logic still behaves
 * correctly for representative URLs.
 */

// NOTE: We assume AMD modules are built/bundled for tests such that this
// path resolves to the updated LessonContentModel module.
// If your build system differs, adjust the import accordingly.
jest.mock('backbone', () => {
  const actualBackbone = jest.requireActual('backbone');
  return {
    ...actualBackbone,
    Model: class extends actualBackbone.Model {}
  };
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({});
});

const Backbone = require('backbone');
const _ = require('underscore');

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;

  beforeAll(() => {
    // Dynamically require after mocks are in place
    // In the real project this path should point to the updated module.
    LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  beforeEach(() => {
    // Ensure clean global URL state for each test
    delete global.document;
    global.document = { URL: '' };
  });

  test('setContent derives lessonUrl by stripping .lesson suffix', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://example.com/intro.lesson/1';

    // Act
    model.setContent('<html>content</html>', false);

    // Assert
    // The regex should replace ".lesson" and any following characters with ".lesson"
    expect(model.get('lessonUrl')).toBe('http://example.com/intro.lesson');
  });

  test('setContent extracts pageNum from URLs matching *.lesson/<1-4 digits>', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://example.com/intro.lesson/123';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('pageNum')).toBe('123');
  });

  test('setContent defaults pageNum to 0 for non-matching URLs', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://example.com/intro.html';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('pageNum')).toBe(0);
  });
});
