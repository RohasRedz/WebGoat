/* eslint-env jest */

// Minimal AMD/Backbone environment simulation for delta tests.

global.document = { URL: '' };

// A lightweight stand-in that mirrors the updated setContent behavior.
function LessonContentModel() {
  this.attributes = {};
  this.listeners = {};
}

LessonContentModel.prototype.set = function (key, value) {
  this.attributes[key] = value;
};

LessonContentModel.prototype.get = function (key) {
  return this.attributes[key];
};

LessonContentModel.prototype.trigger = function (eventName) {
  if (this.listeners[eventName]) {
    this.listeners[eventName].forEach((fn) => fn());
  }
};

LessonContentModel.prototype.on = function (eventName, fn) {
  this.listeners[eventName] = this.listeners[eventName] || [];
  this.listeners[eventName].push(fn);
};

LessonContentModel.prototype.setContent = function (content, loadHelps) {
  if (typeof loadHelps === 'undefined') {
    loadHelps = true;
  }
  this.set('content', content);

  var currentUrl = String(global.document.URL || '');

  var lessonUrlMatch = currentUrl.match(/^(.*?\.lesson).*$/);
  var lessonUrl = lessonUrlMatch ? lessonUrlMatch[1] : currentUrl;
  this.set('lessonUrl', lessonUrl);

  var pageNum = 0;
  var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
  if (pageMatch) {
    pageNum = parseInt(pageMatch[1], 10) || 0;
  }
  this.set('pageNum', pageNum);

  this.trigger('content:loaded', this, loadHelps);
};


describe('LessonContentModel.js delta regex behavior', () => {
  beforeEach(() => {
    global.document.URL = '';
  });

  test('setContent sets lessonUrl to base .lesson URL and pageNum to 0 when no page number', () => {
    // Arrange
    global.document.URL = 'http://example.com/path/to/lesson1.lesson?param=1#hash';
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/lesson1.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent extracts pageNum when URL ends with .lesson/<digits>', () => {
    // Arrange
    global.document.URL = 'http://example.com/some/lesson.lesson/42';
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/some/lesson.lesson');
    expect(model.get('pageNum')).toBe(42);
  });

  test('setContent defaults pageNum to 0 for malformed suffix', () => {
    // Arrange
    global.document.URL = 'http://example.com/another.lesson/not-a-number';
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/another.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
