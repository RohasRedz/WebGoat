// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Delta tests for LessonContentModel.js focusing on the inefficient regex / ReDoS fix.
//
// Before fix (vulnerable):
//   this.set('lessonUrl',document.URL.replace(/\.lesson.*/,'.lesson'));
//   if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
//       this.set('pageNum',document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1'));
//   } else {
//       this.set('pageNum',0);
//   }
//
// After fix (secure, simplified and bounded):
//   var currentUrl = String(document.URL);
//   if (currentUrl.length > 2048) {
//       currentUrl = currentUrl.slice(0, 2048);
//   }
//   var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
//   this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));
//   if (pageNumMatch) { this.set('pageNum', pageNumMatch[1]); } else { this.set('pageNum', 0); }

/* eslint-env jest */

// TODO: adjust module resolution if the AMD module is bundled differently in the real project.
const fs = require('fs');
const path = require('path');

describe('LessonContentModel delta tests', () => {
  let LessonContentModelFactory;
  let modelInstance;
  let BackboneMock;
  let HTMLContentModelMock;

  beforeEach(() => {
    // Minimal Backbone + HTMLContentModel stubs to construct the model.
    BackboneMock = {
      Model: function () {},
    };
    BackboneMock.Model.prototype.fetch = jest.fn().mockReturnValue({
      done: function (cb) {
        cb('<html>content</html>');
        return this;
      },
    });

    HTMLContentModelMock = function () {};
    HTMLContentModelMock.extend = function (definition) {
      function Model() {
        this.attributes = {};
      }
      Model.prototype = {
        set: function (key, value) {
          this.attributes[key] = value;
        },
        get: function (key) {
          return this.attributes[key];
        },
        trigger: jest.fn(),
      };
      Object.assign(Model.prototype, definition);
      return Model;
    };

    // Simulate AMD define wrapper
    global.define = function (deps, factory) {
      LessonContentModelFactory = factory(
        {}, // $
        { escape: (s) => s }, // _
        BackboneMock,
        HTMLContentModelMock
      );
    };

    // Load the AMD module by executing its source
    const scriptPath = path.join(
      __dirname,
      '..',
      '..',
      '..',
      'main',
      'resources',
      'webgoat',
      'static',
      'js',
      'goatApp',
      'model',
      'LessonContentModel.js'
    );
    const code = fs.readFileSync(scriptPath, 'utf8');
    // eslint-disable-next-line no-eval
    eval(code);

    modelInstance = new LessonContentModelFactory();
  });

  afterEach(() => {
    jest.resetAllMocks();
    delete global.define;
  });

  test('setContent() correctly parses pageNum from well-formed URL and sets lessonUrl', () => {
    // Arrange
    global.document = {
      URL: 'https://example.com/test.lesson/42',
    };

    // Act
    modelInstance.setContent('<html>content</html>', true);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe('https://example.com/test.lesson');
    expect(modelInstance.get('pageNum')).toBe('42');
  });

  test('setContent() sets pageNum=0 when URL does not contain page number', () => {
    // Arrange
    global.document = {
      URL: 'https://example.com/test.lesson',
    };

    // Act
    modelInstance.setContent('<html>content</html>', true);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe('https://example.com/test.lesson');
    expect(modelInstance.get('pageNum')).toBe(0);
  });

  test('setContent() safely truncates extremely long URLs to bound regex processing', () => {
    // Arrange
    const longTail = 'x'.repeat(10000);
    global.document = {
      URL: 'https://example.com/test.lesson/1?' + longTail,
    };

    // Act
    modelInstance.setContent('<html>content</html>', true);

    // Assert
    const lessonUrl = modelInstance.get('lessonUrl');
    const pageNum = modelInstance.get('pageNum');

    // The pageNum should still be parsed as '1' from a bounded URL and not cause performance issues.
    expect(lessonUrl.startsWith('https://example.com/test.lesson')).toBe(true);
    expect(pageNum).toBe('1');
  });

  test('loadData() bounds lesson name length and produces safe urlRoot', () => {
    // Arrange
    const veryLongName = 'lesson-' + 'y'.repeat(1000);

    // Act
    modelInstance.loadData({ name: veryLongName });

    // Assert
    const urlRoot = modelInstance.urlRoot;
    expect(urlRoot.endswith('.lesson')).toBe(true);
    // The prefix should be truncated to <= 256 characters before ".lesson" is appended.
    expect(urlRoot.length).toBeLessThanOrEqual(256 + '.lesson'.length);
  });
});
