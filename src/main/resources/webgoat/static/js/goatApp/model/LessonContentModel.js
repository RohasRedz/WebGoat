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
            // Ensure options and options.name are present and strings, then safely encode
            var name = (options && typeof options.name === 'string') ? options.name : '';
            // encodeURIComponent already encodes special characters safely for use in URLs
            // _.escape is removed here to avoid unnecessary double-encoding and complexity
            this.urlRoot = encodeURIComponent(name) + '.lesson';
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

            // Use location.href instead of document.URL for better standards compatibility
            var currentUrl = String(window.location && window.location.href ? window.location.href : '');

            // Precompile and reuse safer, bounded regular expressions
            var lessonUrlPattern = /\.lesson.*/;
            var pageNumPattern = /.*\.lesson\/(\d{1,4})$/;

            this.set('lessonUrl', currentUrl.replace(lessonUrlPattern, '.lesson'));

            var pageMatch = currentUrl.match(pageNumPattern);
            if (pageMatch && pageMatch[1]) {
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
