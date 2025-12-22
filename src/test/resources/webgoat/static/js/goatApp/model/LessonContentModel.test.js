/**
 * Delta tests for LessonContentModel focusing only on the changed behavior:
 * - Regular expression logic for parsing document.URL and extracting:
 *   - lessonUrl (base .lesson URL)
 *   - pageNum (optional trailing 118 digit page number)
 *
 * These tests verify:
 * 1) URL with page number: correct lessonUrl and numeric pageNum.
 * 2) URL without page number: correct lessonUrl and default pageNum = 0.
 */

const _ = require('underscore');
const Backbone = require('backbone');

// Provide a minimal HTMLContentModel stub if the real module is not easily importable.
// TODO: Replace stub with real module path if available in the test environment.
class HTMLContentModel extends Backbone.Model {}

global.define = function (deps, factory) {
  // Simple AMD shim: we ignore deps and just execute factory
  module.exports = factory(require('jquery'), _, Backbone, HTMLContentModel);
};

require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'); // TODO: adjust relative path if project layout differs

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;
  let originalDocumentUrl;

  beforeAll(() => {
    LessonContentModel = module.exports;
  });

  beforeEach(() => {
    originalDocumentUrl = global.document && global.document.URL;
    global.document = { URL: '' };
  });

  afterEach(() => {
    if (originalDocumentUrl !== undefined) {
      global.document.URL = originalDocumentUrl;
    }
  });

  function createModelAndSetContent(url, content, loadHelps) {
    global.document.URL = url;
    const model = new LessonContentModel();
    const spy = jest.fn();
    model.on('content:loaded', spy);
    model.setContent(content, loadHelps);
    return { model, eventSpy: spy };
  }

  test('URL with page number should set correct lessonUrl and numeric pageNum', () => {
    const url = 'https://example.com/app.lesson/12';
    const { model, eventSpy } = createModelAndSetContent(url, '<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('https://example.com/app.lesson');
    expect(model.get('pageNum')).toBe(12);
    expect(eventSpy).toHaveBeenCalledTimes(1);
    const [ctx, loadHelpsFlag] = eventSpy.mock.calls[0];
    expect(ctx).toBe(model);
    expect(loadHelpsFlag).toBe(true);
  });

  test('URL without page number should set lessonUrl to base and pageNum to 0', () => {
    const url = 'https://example.com/app.lesson';
    const { model, eventSpy } = createModelAndSetContent(url, '<html>content</html>');

    expect(model.get('lessonUrl')).toBe('https://example.com/app.lesson');
    expect(model.get('pageNum')).toBe(0);
    expect(eventSpy).toHaveBeenCalledTimes(1);
    const [ctx, loadHelpsFlag] = eventSpy.mock.calls[0];
    expect(ctx).toBe(model);
    expect(loadHelpsFlag).toBe(true);
  });
});
