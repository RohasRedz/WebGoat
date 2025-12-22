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
            // Ensure name is treated as a safe, bounded string before use
            var name = typeof options.name === 'string' ? options.name : '';
            name = name.substring(0, 255); // enforce a sane maximum length

            this.urlRoot = _.escape(encodeURIComponent(name)) + '.lesson';
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

            // Use a safer and simpler pattern with explicit anchors and no nested quantifiers
            var lessonUrlMatch = currentUrl.match(/\.lesson(\/\d{1,4})?$/);
            if (lessonUrlMatch) {
                // Replace only the ending ".lesson" (optionally followed by /<page>) with ".lesson"
                this.set('lessonUrl', currentUrl.replace(/\.lesson(\/\d{1,4})?$/, '.lesson'));
            } else {
                // Fallback: basic normalization without complex regex
                this.set('lessonUrl', currentUrl.split('?')[0]);
            }

            // Extract page number with a bounded, non-backtracking-heavy regex
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch) {
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
