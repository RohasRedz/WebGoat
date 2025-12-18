// Assuming a Jest test located under
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.spec.js
// and AMD module is loaded via a test harness.
// TODO: Adjust module loading mechanism to match actual AMD/RequireJS setup.

const { JSDOM } = require('jsdom');

describe('LessonContentModel.js delta tests', () => {
  let LessonContentModel;
  let modelInstance;
  let originalDocument;

  beforeEach(() => {
    // Minimal JSDOM setup to provide document.URL
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/WebGoat/app.lesson/10'
    });
    originalDocument = global.document;
    global.document = dom.window.document;

    // TODO: In a real project, this would load the AMD module via RequireJS;
    // here we assume CommonJS compatibility or a test shim.
    LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
    modelInstance = new LessonContentModel();
  });

  afterEach(() => {
    global.document = originalDocument;
    jest.resetModules();
  });

  test('computes lessonUrl and pageNum correctly for URL ending with .lesson/number', () => {
    // Arrange
    const content = '<h1>Lesson</h1>';

    // Act
    modelInstance.setContent(content, true);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://localhost/WebGoat/app.lesson'
    );
    expect(modelInstance.get('pageNum')).toBe(10);
  });

  test('computes lessonUrl and pageNum correctly for URL ending with .lesson only', () => {
    // Arrange
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/WebGoat/app.lesson'
    });
    global.document = dom.window.document;
    const content = '<h1>Lesson</h1>';

    // Act
    modelInstance.setContent(content, true);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://localhost/WebGoat/app.lesson'
    );
    expect(modelInstance.get('pageNum')).toBe(0);
  });
});
