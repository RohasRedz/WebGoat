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

            // Use a precompiled, explicit, non-ambiguous regex to avoid catastrophic backtracking
            var url = document.URL;
            // Matches any URL ending with ".lesson" and strips everything after it
            var lessonUrlMatch = url.match(/^(.*?\.lesson)(?:\/.*)?$/);
            if (lessonUrlMatch) {
                this.set('lessonUrl', lessonUrlMatch[1]);
            } else {
                this.set('lessonUrl', url.replace(/\.lesson.*/, '.lesson'));
            }

            // Safe, bounded regex for extracting a 1- to 4-digit page number at the end of the URL
            var pageNumMatch = url.match(/\.lesson\/([0-9]{1,4})$/);
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
