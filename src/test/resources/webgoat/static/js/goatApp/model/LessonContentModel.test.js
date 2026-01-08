// Derived from:
//   Source : src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
//   Test   : src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

jest.mock('backbone', () => {
  const original = jest.requireActual('backbone');
  return {
    ...original,
    Model: class MockModel {
      fetch(options) {
        if (this.__fetchImpl) {
          return this.__fetchImpl(options);
        }
        return Promise.resolve({});
      }
    }
  };
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model;
});

// NOTE: The actual import path may vary depending on bundler setup.
// This assumes a Node-resolvable path equivalent to the source path.
// In the WebGoat build, tests are typically run in a browser-like environment.
// TODO: Adjust module resolution if necessary in the real test runner.
const LessonContentModelFactory = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel delta tests', () => {
  it('should build urlRoot using encodeURIComponent without underscore escaping', () => {
    const LessonContentModel = LessonContentModelFactory;
    const model = new LessonContentModel();

    const fetchMock = jest.fn().mockResolvedValue({});
    model.__fetchImpl = fetchMock;

    model.loadData({ name: 'Lesson 1: Intro & Basics' });

    expect(model.urlRoot).toBe(encodeURIComponent('Lesson 1: Intro & Basics') + '.lesson');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
