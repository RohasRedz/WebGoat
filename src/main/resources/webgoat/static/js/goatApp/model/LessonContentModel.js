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
            // Use safe encoding for the lesson name without double-encoding or unnecessary escaping
            var lessonName = String(options.name || '');
            this.urlRoot = encodeURIComponent(lessonName) + '.lesson';
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

            // Use a simpler, non-backtracking-heavy approach to derive lessonUrl and pageNum
            var currentUrl = String(document.URL || '');

            // Derive lessonUrl by stripping any trailing `.lesson` and optional `/digits`
            // Example:
            //   /foo/bar.lesson/12  -> /foo/bar.lesson
            //   /foo/bar.lesson     -> /foo/bar.lesson
            var lessonUrlMatch = currentUrl.match(/^(.*?\.lesson)(?:\/\d{1,4})?$/);
            if (lessonUrlMatch && lessonUrlMatch[1]) {
                this.set('lessonUrl', lessonUrlMatch[1]);
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Derive pageNum using a lightweight regex without nested or ambiguous quantifiers
            var pageNum = 0;
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch && pageMatch[1]) {
                pageNum = parseInt(pageMatch[1], 10);
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
