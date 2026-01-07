// Assuming this test file lives under a Jest-controlled environment.
// Module path is inferred from original AMD usage; adapt import path as needed.
// TODO: Adjust require path according to actual project test setup if different.
jest.mock('backbone', () => {
  const actual = jest.requireActual('backbone');
  return {
    ...actual,
    Model: actual.Model,
  };
});

jest.mock('underscore', () => ({
  extend: Object.assign,
}));

// We cannot directly require the AMD module without a loader; instead, we
// simulate the core logic change (regex and URL parsing) by requiring the
// built bundle or by extracting the function. For this delta test, we assume
// the AMD module is made CommonJS-compatible in tests.
// TODO: Replace with actual path to the compiled/bundled module if necessary.
const jsdom = require('jsdom');
const { JSDOM } = jsdom;

// Minimal shim to import the module in a Jest environment.
// If your build exposes LessonContentModel via a bundle, replace this with that import.
// For now, we re-require the original AMD file through a pre-bundled output.
// TODO: Update path if your test setup differs.
let LessonContentModel;
beforeAll(() => {
  // eslint-disable-next-line global-require
  LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'); // TODO: adjust path
});

describe('LessonContentModel delta tests (regex & URL handling)', () => {
  test('setContent parses pageNum from URL with .lesson/<digits> suffix using updated regex', () => {
    // Arrange
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/path/challenge.lesson/1234',
    });
    global.document = dom.window.document;
    global.window = dom.window;

    // Instantiate model
    const model = new LessonContentModel();

    // Spy on trigger to ensure it is called (unchanged behavior)
    const triggerSpy = jest.spyOn(model, 'trigger').mockImplementation(() => {});

    // Act
    model.setContent('<div>content</div>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'http://example.com/path/challenge.lesson'
    );
    expect(model.get('pageNum')).toBe('1234');
    expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
  });

  test('setContent sets pageNum to 0 when URL does not match .lesson/<digits> pattern', () => {
    // Arrange
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/path/challenge.lesson',
    });
    global.document = dom.window.document;
    global.window = dom.window;

    const model = new LessonContentModel();
    const triggerSpy = jest.spyOn(model, 'trigger').mockImplementation(() => {});

    // Act
    model.setContent('<div>content</div>', false);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'http://example.com/path/challenge.lesson'
    );
    expect(model.get('pageNum')).toBe(0);
    expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, false);
  });

  test('loadData encodes options.name safely into urlRoot', () => {
    // Arrange
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/path/challenge.lesson',
    });
    global.document = dom.window.document;
    global.window = dom.window;

    const model = new LessonContentModel();

    // Mock fetch to avoid network and to validate URL / options behavior
    const fetchMock = jest.spyOn(model, 'fetch').mockImplementation(function () {
      return {
        done: (cb) => {
          cb('<div>dummy</div>');
          return this;
        },
      };
    });

    // Act
    model.loadData({ name: 'my lesson/with spaces' });

    // Assert
    expect(model.urlRoot).toBe(
      encodeURIComponent('my lesson/with spaces') + '.lesson'
    );
    expect(fetchMock).toHaveBeenCalled();
    expect(model.get('content')).toBe('<div>dummy</div>');
  });
});
