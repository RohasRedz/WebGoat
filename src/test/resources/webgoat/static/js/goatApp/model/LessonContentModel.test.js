const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

jest.mock('jquery', () => ({}));
jest.mock('underscore', () => ({
  escape: (s) => s
}));
jest.mock('backbone', () => {
  const Backbone = {
    Model: function () {},
  };
  Backbone.Model.prototype = {
    fetch: jest.fn().mockResolvedValue({}),
    set: jest.fn(),
    trigger: jest.fn()
  };
  Backbone.Model.prototype.fetch.call = Function.prototype.call;
  return Backbone;
});

const defineLessonModule = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel URL and page parsing (delta test)', () => {
  function getLessonContentModel() {
    let exported;
    global.define = (deps, factory) => {
      exported = factory($, _, Backbone, Backbone.Model);
    };
    defineLessonModule;
    return exported;
  }

  test('setContent should set lessonUrl and numeric pageNum when URL ends with .lesson/<digits>', () => {
    const LessonContentModel = getLessonContentModel();
    const model = new LessonContentModel();
    model.set = jest.fn();
    model.trigger = jest.fn();

    const originalUrl = 'http://example.com/path/to/topic.lesson/12';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    try {
      model.setContent('<html>content</html>', true);

      expect(model.set).toHaveBeenCalledWith('lessonUrl', 'http://example.com/path/to/topic.lesson');
      expect(model.set).toHaveBeenCalledWith('pageNum', 12);
    } finally {
      global.document = originalDocument;
    }
  });

  test('setContent should set pageNum to 0 when URL does not match .lesson/<digits>', () => {
    const LessonContentModel = getLessonContentModel();
    const model = new LessonContentModel();
    model.set = jest.fn();
    model.trigger = jest.fn();

    const originalUrl = 'http://example.com/path/to/topic.lesson';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    try {
      model.setContent('<html>content</html>', true);

      expect(model.set).toHaveBeenCalledWith('lessonUrl', 'http://example.com/path/to/topic.lesson');
      expect(model.set).toHaveBeenCalledWith('pageNum', 0);
    } finally {
      global.document = originalDocument;
    }
  });
});
