define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Compiled and pre-validated regular expressions to avoid inefficient patterns
    // URL pattern: match any characters up to `.lesson`, in a non-greedy way
    var LESSON_URL_PATTERN = new RegExp("\\.lesson.*$");
    // Page number pattern: match a trailing segment /<1-4 digits> with no catastrophic backtracking
    var PAGE_NUMBER_PATTERN = new RegExp(".*\\.lesson\\/(\\d{1,4})$");

    return HTMLContentModel.extend({
        urlRoot:null,
        defaults: {
            items:null,
            selectedItem:null
        },

        initialize: function (options) {

        },

        loadData: function(options) {
            // Ensure the name is a string and reasonably bounded before encoding
            var safeName = '';
            if (options && typeof options.name === 'string') {
                // Limit length to mitigate potential ReDoS or performance issues with extremely long input
                safeName = options.name.substring(0, 256);
            }
            this.urlRoot = encodeURIComponent(safeName) + '.lesson';
            var self = this;
            this.fetch().done(function(data) {
                self.setContent(data);
            });
        },

        setContent: function(content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content',content);

            var currentUrl = String(document.URL || '');

            // Use precompiled, simple regex to avoid complex backtracking
            this.set('lessonUrl', currentUrl.replace(LESSON_URL_PATTERN, '.lesson'));

            // Safely extract page number using the precompiled regex
            var pageNum = 0;
            var match = currentUrl.match(PAGE_NUMBER_PATTERN);
            if (match && match[1]) {
                pageNum = parseInt(match[1], 10);
                if (!Number.isFinite(pageNum) || pageNum < 0) {
                    pageNum = 0;
                }
            }
            this.set('pageNum', pageNum);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
