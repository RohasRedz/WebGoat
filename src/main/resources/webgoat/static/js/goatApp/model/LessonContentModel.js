define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Precompile safe, linear-time regular expressions and reuse them.
    // These patterns avoid ambiguous constructs that can cause excessive backtracking.
    var LESSON_URL_REGEX = /\.lesson(?:\/\d{1,4})?$/;
    var PAGE_NUM_REGEX = /(?:^|\/)(\d{1,4})$/;

    return HTMLContentModel.extend({
        urlRoot:null,
        defaults: {
            items:null,
            selectedItem:null
        },

        initialize: function (options) {

        },

        loadData: function(options) {
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
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

            // Use precompiled, efficient regex to normalize the lesson URL
            var normalizedUrl = document.URL;
            if (LESSON_URL_REGEX.test(normalizedUrl)) {
                normalizedUrl = normalizedUrl.replace(LESSON_URL_REGEX, '.lesson');
            }
            this.set('lessonUrl', normalizedUrl);

            // Use precompiled, efficient regex to extract page number safely
            var pageNum = 0;
            var pageMatch = PAGE_NUM_REGEX.exec(document.URL);
            if (pageMatch && pageMatch[1]) {
                pageNum = parseInt(pageMatch[1], 10);
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
