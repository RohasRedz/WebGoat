define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Precompile safe, linear-time regexes to avoid inefficient backtracking
    // Matches trailing ".lesson" segment (no page number)
    var LESSON_URL_BASE_REGEX = /\.lesson(?:\/.*)?$/;
    // Matches URLs ending in ".lesson/<118 digit page number>"
    var LESSON_URL_PAGE_REGEX = /\.lesson\/(\d{1,4})$/;

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

            // Replace the original ad200hoc regexes with precompiled, efficient patterns
            // Original behavior:
            //  - lessonUrl: document.URL.replace(/\.lesson.*/,'.lesson')
            //  - if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
            //        pageNum: document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1')
            //    } else { pageNum: 0 }
            var currentUrl = String(document.URL || '');

            // Derive the base lesson URL deterministically using a safe regex
            this.set('lessonUrl', currentUrl.replace(LESSON_URL_BASE_REGEX, '.lesson'));

            // Extract page number using a capture group without backtracking128heavy patterns
            var pageNum = 0;
            var match = LESSON_URL_PAGE_REGEX.exec(currentUrl);
            if (match && match[1]) {
                pageNum = parseInt(match[1], 10);
                if (!Number.isFinite(pageNum)) {
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
