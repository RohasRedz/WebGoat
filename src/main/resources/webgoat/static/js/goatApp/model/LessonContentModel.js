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

            // Use a more constrained and efficient pattern to strip the .lesson suffix
            // Instead of /.lesson.*/, explicitly match the .lesson suffix at the end if present
            var currentUrl = document.URL;
            this.set('lessonUrl', currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson'));

            // Use a stricter, linear-time friendly pattern to capture the page number
            // - Anchor the pattern to the end of the string
            // - Avoid a leading greedy .*
            // - Restrict the path characters before ".lesson" to non-space characters
            var pageMatch = currentUrl.match(/\/[^/\s]+\.lesson\/(\d{1,4})$/);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
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
