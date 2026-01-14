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

            // Use URL API for robust parsing and avoid complex regex on document.URL
            var currentUrl;
            try {
                currentUrl = new URL(document.URL);
            } catch (e) {
                // Fallback if URL constructor is not supported; keep original behavior
                this.set('lessonUrl',document.URL.replace(/\.lesson.*/,'.lesson'));
                if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
                    this.set('pageNum',document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1'));
                } else {
                    this.set('pageNum',0);
                }
                this.trigger('content:loaded',this,loadHelps);
                return;
            }

            // Derive lessonUrl from pathname instead of regex over full URL
            var lessonPathname = currentUrl.pathname.replace(/\.lesson.*/, '.lesson');
            var lessonUrl = currentUrl.origin + lessonPathname;
            this.set('lessonUrl', lessonUrl);

            // Extract pageNum using simple, bounded parsing without catastrophic-regex patterns
            var pageNum = 0;
            var pathSegments = currentUrl.pathname.split('/');
            var lastSegment = pathSegments[pathSegments.length - 1];

            // expected patterns:
            //   *.lesson/<number>
            //   or just *.lesson
            var lastSegmentNumberMatch = lastSegment.match(/^(\d{1,4})$/);
            if (lastSegmentNumberMatch) {
                pageNum = parseInt(lastSegmentNumberMatch[1], 10);
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
