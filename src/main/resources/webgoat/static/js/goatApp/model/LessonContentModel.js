define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    /**
     * Extract the page number from a lesson URL using a safe, bounded approach
     * to avoid inefficient regular expression backtracking.
     *
     * @param {string} url
     * @returns {number}
     */
    function extractPageNum(url) {
        if (typeof url !== 'string' || url.length > 2048) {
            // Defensive limit to avoid processing extremely long inputs
            return 0;
        }

        // Normalize to a predictable suffix `.lesson/<digits>`
        var lessonSuffixIndex = url.indexOf('.lesson/');
        if (lessonSuffixIndex === -1) {
            return 0;
        }

        var suffix = url.substring(lessonSuffixIndex + '.lesson/'.length);

        // Accept only up to 4 digits to match original intent and avoid ReDoS
        var pageMatch = /^(\d{1,4})$/.exec(suffix);
        if (pageMatch) {
            return parseInt(pageMatch[1], 10);
        }

        return 0;
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

            // Use a simpler, deterministic replacement instead of a complex regex
            var currentUrl = document.URL;
            var lessonIndex = currentUrl.indexOf('.lesson');
            if (lessonIndex !== -1) {
                this.set('lessonUrl', currentUrl.substring(0, lessonIndex + '.lesson'.length));
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Use safe helper to extract page number and avoid inefficient regex complexity
            this.set('pageNum', extractPageNum(currentUrl));

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
