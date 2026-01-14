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

            // Use URL API for robust parsing and avoid overly complex regular expressions
            var currentUrl = new URL(document.URL, window.location.origin);
            var pathname = currentUrl.pathname;

            // Derive lessonUrl by replacing the trailing ".lesson..." segment with ".lesson"
            // without using a complex backtracking-prone regex
            var lessonUrl = pathname;
            var lessonIndex = lessonUrl.indexOf('.lesson');
            if (lessonIndex !== -1) {
                lessonUrl = lessonUrl.substring(0, lessonIndex + '.lesson'.length);
            }
            this.set('lessonUrl', lessonUrl);

            // Safely extract page number: expect pattern ending with ".lesson/<digits>"
            var pageNum = 0;
            var pathSegments = pathname.split('/');
            var lastSegment = pathSegments[pathSegments.length - 1];

            if (pathname.indexOf('.lesson/') !== -1 && /^[0-9]{1,4}$/.test(lastSegment)) {
                pageNum = parseInt(lastSegment, 10);
                if (Number.isNaN(pageNum)) {
                    pageNum = 0;
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
