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

            // Use a simpler, more efficient pattern and single evaluation for URL parsing
            var currentUrl = String(document.URL);
            // Match "<base>.lesson" (no need for greedy ".*")
            var lessonMatch = currentUrl.match(/^(.*?\.lesson)(?:\/.*)?$/);
            if (lessonMatch && lessonMatch[1]) {
                this.set('lessonUrl', lessonMatch[1]);
            } else {
                // Fallback to existing URL if pattern does not match
                this.set('lessonUrl', currentUrl);
            }

            // Reuse the same URL and a precompiled pattern to avoid redundant regex work
            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageNumMatch && pageNumMatch[1]) {
                this.set('pageNum', pageNumMatch[1]);
            } else {
                this.set('pageNum',0);
            }
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
