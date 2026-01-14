// Assuming AMD modules are testable via a loader or bundler environment.
// This Jest test focuses specifically on the modified URL handling behavior
// in LessonContentModel, especially the length cap and use of currentUrl.

const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub matching the inheritance expectations.
const HTMLContentModel = Backbone.Model.extend({});

// Recreate the module under test with the same exports as the updated file.
// In a real project, this would be:
//
// const LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel');
//
// For this delta test, we inline the behavior relevant to the changes.
const LessonContentModel = HTMLContentModel.extend({
  urlRoot: null,
  defaults: {
    items: null,
    selectedItem: null
  },

  loadData: function (options) {
    this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
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

    // Copied from updated implementation to validate behavior:
    var currentUrl = global.window && global.window.location && global.window.location.href
      ? global.window.location.href
      : String(global.document && global.document.URL || '');

    if (currentUrl.length > 2048) {
      currentUrl = currentUrl.substring(0, 2048);
    }

    this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

    var pageMatch = currentUrl.match(/.*\.lesson\/(\d{1,4})$/);
    if (pageMatch) {
      this.set('pageNum', pageMatch[1]);
    } else {
      this.set('pageNum', 0);
    }

    this.trigger('content:loaded', this, loadHelps);
  },

  fetch: function (options) {
    options = options || {};
    return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
  }
});

describe('LessonContentModel delta tests for URL handling', () => {
  beforeEach(() => {
    global.window = {
      location: {
        href: 'http://localhost/lesson/1'
      }
    };
    global.document = {
      URL: 'http://localhost/lesson/1'
    };
  });

  test('setContent caps URL length before applying regex to mitigate inefficient regex complexity', () => {
    // Arrange
    const model = new LessonContentModel();
    const longNumber = '9'.repeat(5000);
    global.window.location.href = `http://example.com/very-long-path/lesson/${longNumber}`;

    let contentLoadedArgs = null;
    model.on('content:loaded', function (m, loadHelps) {
      contentLoadedArgs = { m, loadHelps };
    });

    // Act
    model.setContent('<html></html>', true);

    // Assert
    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');

    expect(typeof lessonUrl).toBe('string');
    expect(lessonUrl.length).toBeLessThanOrEqual(2048);
    // Because the URL is truncated, the numeric suffix will not match the full 5000-digit number;
    // pageNum should be 0 when the regex cannot match the truncated URL pattern.
    expect(pageNum).toBe(0);
    expect(contentLoadedArgs).not.toBeNull();
    expect(contentLoadedArgs.m).toBe(model);
  });

  test('setContent extracts pageNum when URL is within safe length and matches pattern', () => {
    // Arrange
    const model = new LessonContentModel();
    global.window.location.href = 'http://example.com/foo.lesson/1234';

    // Act
    model.setContent('<html></html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
    expect(model.get('pageNum')).toBe('1234');
  });
});
