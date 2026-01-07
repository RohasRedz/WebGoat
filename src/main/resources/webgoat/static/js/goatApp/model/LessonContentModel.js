define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Precompiled, constrained regular expressions for lesson URL and page number
    // Matches a URL ending in ".lesson" (no catastrophic backtracking)
    var LESSON_URL_REGEX = /\.lesson(?:$|[?#])/;
    // Matches a URL ending in ".lesson/<1-4 digit page>" with only digits in the group
    var PAGE_NUM_REGEX = /.*\.lesson\/(\d{1,4})$/;

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

            var currentUrl = String(document.URL || '');

            // Use precompiled, constrained regex for lesson URL
            if (LESSON_URL_REGEX.test(currentUrl)) {
                this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Use precompiled, constrained regex for page number extraction
            if (PAGE_NUM_REGEX.test(currentUrl)) {
                this.set('pageNum', currentUrl.replace(PAGE_NUM_REGEX, '$1'));
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
