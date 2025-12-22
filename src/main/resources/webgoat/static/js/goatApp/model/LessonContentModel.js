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
            // Ensure `options.name` is a reasonable, bounded string to avoid ReDoS and overlong inputs
            var lessonName = typeof options.name === 'string' ? options.name : '';
            // Limit length and allowed characters to reduce risk of regex/path abuse
            lessonName = lessonName.slice(0, 128).replace(/[^a-zA-Z0-9_\-./]/g, '');

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

            // Use precompiled, simple regexes to avoid catastrophic backtracking
            var lessonUrlRegex = /\.lesson.*/;
            var pageNumRegex = /.*\.lesson\/(\d{1,4})$/;

            this.set('lessonUrl', document.URL.replace(lessonUrlRegex, '.lesson'));

            var pageMatch = document.URL.match(pageNumRegex);
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
