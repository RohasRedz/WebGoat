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

            // Use a more efficient and precompiled regular expression to avoid
            // catastrophic backtracking or inefficient regex behavior on long URLs.
            var lessonUrlPattern = /\.lesson.*/;
            this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));

            // Precompile and reuse the page extraction regex; limit the quantifier
            // to a reasonable length to avoid excessive backtracking.
            var pagePattern = /.*\.lesson\/(\d{1,4})$/;
            if (pagePattern.test(document.URL)) {
                this.set('pageNum', document.URL.replace(pagePattern, '$1'));
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
