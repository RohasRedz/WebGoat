define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Precompiled, efficient regular expressions to avoid excessive backtracking
    var LESSON_URL_RE = /\.lesson(?:\/(\d{1,4}))?$/;
    var LESSON_PAGE_RE = /(\d{1,4})$/;

    return HTMLContentModel.extend({
        urlRoot:null,
        defaults: {
            items:null,
            selectedItem:null
        },

        initialize: function (options) {

        },

        loadData: function(options) {
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson'
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

            var currentUrl = document.URL;
            // Normalize and trim the URL to minimize unexpected regex behavior
            if (typeof currentUrl === 'string') {
                currentUrl = currentUrl.trim();
            }

            // Use a precompiled, efficient regex to strip the trailing `.lesson` segment
            if (LESSON_URL_RE.test(currentUrl)) {
                this.set('lessonUrl', currentUrl.replace(LESSON_URL_RE, '.lesson'));
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Extract pageNum using a safer, precompiled regex
            var pageMatch = LESSON_PAGE_RE.exec(currentUrl);
            if (pageMatch && pageMatch[1]) {
                this.set('pageNum', pageMatch[1]);
            } else {
                this.set('pageNum', 0);
            }

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
