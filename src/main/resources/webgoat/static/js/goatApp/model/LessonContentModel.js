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

            // Use a stricter, linear-time regex and pre-validate the URL to avoid inefficient backtracking
            var currentUrl = String(document.URL || '');
            var isLessonUrl = /\.lesson(\/\d{1,4})?$/.test(currentUrl);

            if (isLessonUrl) {
                this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

                var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
                if (pageMatch && pageMatch[1]) {
                    this.set('pageNum', pageMatch[1]);
                } else {
                    this.set('pageNum', 0);
                }
            } else {
                this.set('lessonUrl', currentUrl);
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
