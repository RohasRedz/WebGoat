define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    function getSafeLessonUrl(url) {
        // Enforce basic format and length limits to mitigate ReDoS risk
        if (typeof url !== 'string') {
            return '';
        }

        // Limit length to a reasonable maximum to avoid pathological inputs
        var MAX_URL_LENGTH = 2048;
        if (url.length > MAX_URL_LENGTH) {
            url = url.substring(0, MAX_URL_LENGTH);
        }

        // Use a simpler, bounded pattern and avoid backtracking-heavy constructs
        // Expected formats:
        //   ...something.lesson
        //   ...something.lesson/1234   (1–4 digits)
        //
        // We derive the base lesson URL and page number with lightweight logic
        var lessonMatch = url.match(/^(.*?\.lesson)(?:\/(\d{1,4}))?$/);
        if (!lessonMatch) {
            return {
                lessonUrl: url,
                pageNum: 0
            };
        }

        return {
            lessonUrl: lessonMatch[1],
            pageNum: lessonMatch[2] ? parseInt(lessonMatch[2], 10) : 0
        };
    }

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

            var safe = getSafeLessonUrl(document.URL);
            this.set('lessonUrl', safe.lessonUrl);
            this.set('pageNum', safe.pageNum);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
