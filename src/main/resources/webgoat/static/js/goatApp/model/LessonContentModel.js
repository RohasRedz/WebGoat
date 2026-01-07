define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    function getCurrentPageNum(url) {
        // Safe, bounded numeric extraction without complex backtracking
        var match = /\.lesson\/(\d{1,4})$/.exec(url);
        if (!match) {
            return 0;
        }
        var value = parseInt(match[1], 10);
        if (!Number.isFinite(value) || value < 0 || value > 9999) {
            return 0;
        }
        return value;
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
            this.set('lessonUrl',document.URL.replace(/\.lesson.*/,'.lesson'));
            this.set('pageNum', getCurrentPageNum(document.URL));
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
