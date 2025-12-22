// Jest delta tests for LessonContentModel.js
// Focus: only the changed behavior around regex usage / URL handling.
// Assumes AMD module is bundled/available via require path; adjust if needed.
// TODO: Adjust require path to match actual build if different.
const Backbone = require('backbone');
const _ = require('underscore');

// Minimal stub for HTMLContentModel to satisfy the extension chain.
class HTMLContentModel extends Backbone.Model {}

jest.mock('goatApp/model/HTMLContentModel', () => {
  return HTMLContentModel;
});

describe('LessonContentModel delta tests', () => {
  // Load the module under test via AMD-like pattern simulated in Jest environment.
  let LessonContentModel;

  beforeAll(() => {
    // The original file is AMD-style; in real build it would be transformed.
    // Here we simulate by requiring the transpiled/bundled output.
    // TODO: Replace with the actual module path if different.
    LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  test('setContent computes lessonUrl using bounded regex without altering base path semantics', () => {
    const model = new LessonContentModel();
    const triggerSpy = jest.spyOn(model, 'trigger');

    // Document URL with additional path elements and query to ensure regex is bounded.
    const originalUrl =
      'http://localhost:8080/WebGoat/lesson/SQLInjection.lesson/1?foo=bar#section';
    delete global.document;
    global.document = { URL: originalUrl };

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe(
      'http://localhost:8080/WebGoat/lesson/SQLInjection.lesson'
    );

    // Ensure pageNum is still derived correctly from bounded pattern
    expect(model.get('pageNum')).toBe('1');
    expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
  });

  test('setContent sets pageNum to 0 when URL does not match the bounded pattern', () => {
    const model = new LessonContentModel();
    const triggerSpy = jest.spyOn(model, 'trigger');

    // URL without trailing numeric page segment
    const originalUrl =
      'http://localhost:8080/WebGoat/lesson/SQLInjection.lesson';
    delete global.document;
    global.document = { URL: originalUrl };

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe(
      'http://localhost:8080/WebGoat/lesson/SQLInjection.lesson'
    );
    expect(model.get('pageNum')).toBe(0);
    expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
  });
});
