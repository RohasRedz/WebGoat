define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

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

            var currentUrl = document.URL || '';

            // Derive lessonUrl by stripping any trailing '/
            // Derive lessonUrl by stripping any trailing '/
            // Derive lessonUrl by stripping any trailing '/<page>' pages from the URL
            var lessonUrl = currentUrl.replace(/\/\d{1,4}$/, '');
            lessonUrl = lessonUrl.replace(/\.lesson.*/, '.lesson');
            this.set('lessonUrl', lessonUrl);

            // Efficiently extract page number using indexOf / substring instead of
            // complex, potentially backtracking-prone regular expressions.
            var pageNum = 0;
            var lastSlashIndex = currentUrl.lastIndexOf('/');
            if (lastSlashIndex !== -1 && lastSlashIndex < currentUrl.length - 1) {
                var pageCandidate = currentUrl.substring(lastSlashIndex + 1);
                // Ensure the candidate is strictly 14 digits
                if (/^\d{1,4}$/.test(pageCandidate)) {
                    pageNum = parseInt(pageCandidate, 10);
                    if (!Number.isFinite(pageNum)) {
                        pageNum = 0;
                    }
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
