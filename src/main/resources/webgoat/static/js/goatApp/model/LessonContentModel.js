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
            // Ensure the lesson name is treated as plain text and encoded safely
            var safeName = encodeURIComponent(String(options.name || ''));
            this.urlRoot = safeName + '.lesson';
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

            // Use safe, linear-time-compatible regexes and reuse them
            var url = String(document.URL || '');

            // Replace any ".lesson" suffix with ".lesson" — explicitly match literal dot
            this.set('lessonUrl', url.replace(/\.lesson.*/, '.lesson'));

            // Extract page number using a non-ambiguous pattern with explicit anchors
            var pageMatch = url.match(/\.lesson\/(\d{1,4})$/);
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
