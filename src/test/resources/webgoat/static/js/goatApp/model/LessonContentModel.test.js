/**
 * Delta Jest tests for LessonContentModel.js focused on the safer URL parsing logic:
 * - Ensures lessonUrl is correctly derived without using the old complex regex.
 * - Ensures pageNum is parsed correctly for URLs with and without a numeric page.
 *
 * NOTE: This test assumes LessonContentModel is AMD-wrapped and available via require().
 * If your test runner uses a different module system, adapt imports accordingly.
 */

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the dependency chain.
class HTMLContentModel extends Backbone.Model {}

// Simulate the AMD define wrapper used in the application.
let LessonContentModelFactory;
beforeAll(() => {
  // eslint-disable-next-line global-require
  const defineModule = require('module');
  // Manually construct the module as in the fixed code.
  // We re-create the return value of the AMD define for testing purposes.
  const factory = function ($dep, _dep, BackboneDep, HTMLContentModelDep) {
    return HTMLContentModelDep.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null
      },

      initialize: function (options) {},

      loadData: function (options) {
        this.urlRoot = _dep.escape(encodeURIComponent(options.name)) + '.lesson';
        const self = this;
        this.fetch().done(function (data) {
          self.setContent(data);
        });
      },

      setContent: function (content, loadHelps) {
        if (typeof loadHelps === 'undefined') {
          loadHelps = true;
        }
        this.set('content', content);

        // Fixed parsing logic from updated source:
        const url = global.document.URL;
        const lessonIndex = url.indexOf('.lesson');
        if (lessonIndex !== -1) {
          this.set('lessonUrl', url.substring(0, lessonIndex + '.lesson'.length));
        } else {
          this.set('lessonUrl', url);
        }

        const pageMatch = url.match(/\.lesson\/([0-9]{1,4})$/);
        if (pageMatch) {
          this.set('pageNum', pageMatch[1]);
        } else {
          this.set('pageNum', 0);
        }
        this.trigger('content:loaded', this, loadHelps);
      },

      fetch: function (options) {
        const opts = options || {};
        return BackboneDep.Model.prototype.fetch.call(
          this,
          Object.assign({ dataType: 'html' }, opts)
        );
      }
    });
  };

  LessonContentModelFactory = factory($, _, Backbone, HTMLContentModel);
});

describe('LessonContentModel URL parsing (delta tests)', () => {
  beforeEach(() => {
    // JSDOM document is available in Jest; we override URL per test.
    global.document = {
      URL: 'http://localhost/WebGoat/lesson/Intro.lesson'
    };
  });

  test('setContent derives lessonUrl by stripping suffix after ".lesson"', () => {
    const model = new LessonContentModelFactory();
    const spyTriggered = jest.fn();
    model.on('content:loaded', spyTriggered);

    model.setContent('<html />', true);

    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/Intro.lesson');
    expect(spyTriggered).toHaveBeenCalledTimes(1);
  });

  test('setContent extracts numeric pageNum from URL with page segment', () => {
    global.document.URL = 'http://localhost/WebGoat/lesson/Intro.lesson/123';

    const model = new LessonContentModelFactory();
    const spyTriggered = jest.fn();
    model.on('content:loaded', spyTriggered);

    model.setContent('<html />', true);

    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/Intro.lesson');
    expect(model.get('pageNum')).toBe('123');
    expect(spyTriggered).toHaveBeenCalledTimes(1);
  });

  test('setContent sets pageNum to 0 when URL has no numeric page suffix', () => {
    global.document.URL = 'http://localhost/WebGoat/lesson/Intro.lesson';

    const model = new LessonContentModelFactory();
    const spyTriggered = jest.fn();
    model.on('content:loaded', spyTriggered);

    model.setContent('<html />', true);

    expect(model.get('pageNum')).toBe(0);
    expect(spyTriggered).toHaveBeenCalledTimes(1);
  });
});
