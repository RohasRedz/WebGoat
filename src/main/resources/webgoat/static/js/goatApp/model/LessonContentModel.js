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

            // Use a more precise and efficient pattern:
            // - Match up to the first occurrence of ".lesson" and keep it.
            // - Avoid overly broad ".*" after .lesson which can lead to inefficient backtracking.
            this.set('lessonUrl', document.URL.replace(/(\.lesson).*$/, '$1'));

            // Replace inefficient leading ".*" in the page number regex with an anchored, simpler pattern:
            // - ^[^?]*\.lesson\/(\d{1,4})$ :
            //   ^        : start of string
            //   [^?]*    : any characters up to (but not including) a query string '?'
            //   \.lesson/: literal ".lesson/"
            //   (\d{1,4}): capture 1–4 digit page number
            //   $        : end of string
            var pageMatch = document.URL.match(/^[^?]*\.lesson\/(\d{1,4})$/);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
            } else {
                this.set('pageNum',0);
            }
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
