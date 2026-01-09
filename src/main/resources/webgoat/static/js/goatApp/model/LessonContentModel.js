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

            // Hardened URL parsing with safer, precompiled regex patterns
            var currentUrl = document.URL;

            // Use a simple, non-backtracking-prone pattern for the lesson URL
            var lessonUrlMatch = currentUrl.match(/\.lesson/);
            if (lessonUrlMatch) {
                this.set('lessonUrl', currentUrl.substring(0, lessonUrlMatch.index + '.lesson'.length));
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Use a safe, bounded regex for page number extraction
            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageNumMatch) {
                this.set('pageNum', pageNumMatch[1]);
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
