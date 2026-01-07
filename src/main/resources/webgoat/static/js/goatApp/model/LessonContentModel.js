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

            var currentUrl = document.URL;
            // Derive lesson base URL without using complex or potentially backtracking-prone regexes
            var lessonIndex = currentUrl.indexOf('.lesson');
            var lessonUrl;
            if (lessonIndex !== -1) {
                lessonUrl = currentUrl.substring(0, lessonIndex + '.lesson'.length);
            } else {
                lessonUrl = currentUrl;
            }
            this.set('lessonUrl', lessonUrl);

            // Safely extract page number by simple parsing instead of heavy regex
            var pageNum = 0;
            // Expecting URLs like: something.lesson/12 or something.lesson/3
            var lessonSuffix = currentUrl.split('.lesson/')[1];
            if (lessonSuffix) {
                var candidate = lessonSuffix.split(/[^\d]/)[0]; // take leading digits only
                if (candidate && /^\d{1,4}$/.test(candidate)) {
                    pageNum = parseInt(candidate, 10);
                }
            }
            this.set('pageNum', pageNum);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
