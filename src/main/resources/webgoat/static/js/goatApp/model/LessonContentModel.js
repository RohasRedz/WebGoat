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

            // Use safer, simple substring checks to derive lessonUrl and pageNum
            var currentUrl = document.URL || "";
            var lessonSuffix = '.lesson';
            var lessonSuffixWithSlash = '.lesson/';
            var lessonIndex = currentUrl.indexOf(lessonSuffix);

            if (lessonIndex !== -1) {
                // Everything up to and including ".lesson" is the base lesson URL
                var baseLessonUrl = currentUrl.substring(0, lessonIndex + lessonSuffix.length);
                this.set('lessonUrl', baseLessonUrl);

                var remaining = currentUrl.substring(lessonIndex + lessonSuffix.length);
                // When there is a trailing "/<digits>" after ".lesson"
                if (remaining.indexOf('/') === 0) {
                    var pagePart = remaining.substring(1);
                    // Accept only purely numeric page numbers of reasonable length (1-4 digits)
                    if (/^[0-9]{1,4}$/.test(pagePart)) {
                        this.set('pageNum', pagePart);
                    } else {
                        this.set('pageNum', 0);
                    }
                } else {
                    this.set('pageNum', 0);
                }
            } else {
                // Fallbacks if URL does not contain ".lesson"
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
