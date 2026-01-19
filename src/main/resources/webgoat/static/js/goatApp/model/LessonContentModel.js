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

            // Normalize URL once to reduce repeated regex work
            var currentUrl = String(document.URL || '');

            this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

            // Safer and more efficient page number extraction:
            // - Avoid catastrophic backtracking by simplifying the pattern.
            // - Avoid running two complex regexes sequentially on the full URL.
            var pageNum = 0;
            var lessonMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (lessonMatch && lessonMatch[1]) {
                pageNum = parseInt(lessonMatch[1], 10);
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
