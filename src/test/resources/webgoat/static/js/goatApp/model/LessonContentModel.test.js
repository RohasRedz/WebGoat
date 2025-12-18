jest.mock('goatApp/model/HTMLContentModel', () => {
  return class HTMLContentModel {
    constructor(attrs) {
      this.attributes = attrs || {};
    }
    set(key, value) {
      this.attributes[key] = value;
    }
    get(key) {
      return this.attributes[key];
    }
    trigger() {}
    fetch() {
      return { done: () => {} };
    }
  };
});

const Backbone = require('backbone');
global.Backbone = Backbone;
const _ = require('underscore');
global._ = _;
const $ = require('jquery');
global.$ = $;

const LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel delta tests - URL parsing', () => {
  test('setContent sets lessonUrl and pageNum correctly for URL without page number', () => {
    const model = new LessonContentModel();
    delete window.location;
    window.location = { href: 'http://example.com/SomeLesson.lesson' };
    Object.defineProperty(document, 'URL', {
      configurable: true,
      get: () => 'http://example.com/SomeLesson.lesson'
    });

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent sets lessonUrl and pageNum correctly for URL with numeric page number', () => {
    const model = new LessonContentModel();
    delete window.location;
    window.location = { href: 'http://example.com/SomeLesson.lesson/12' };
    Object.defineProperty(document, 'URL', {
      configurable: true,
      get: () => 'http://example.com/SomeLesson.lesson/12'
    });

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(12);
  });

  test('setContent treats non-numeric tail as no page number (pageNum=0)', () => {
    const model = new LessonContentModel();
    delete window.location;
    window.location = { href: 'http://example.com/SomeLesson.lesson/abc' };
    Object.defineProperty(document, 'URL', {
      configurable: true,
      get: () => 'http://example.com/SomeLesson.lesson/abc'
    });

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent only accepts up to 4 digit page numbers', () => {
    const model = new LessonContentModel();
    delete window.location;
    window.location = { href: 'http://example.com/SomeLesson.lesson/12345' };
    Object.defineProperty(document, 'URL', {
      configurable: true,
      get: () => 'http://example.com/SomeLesson.lesson/12345'
    });

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
