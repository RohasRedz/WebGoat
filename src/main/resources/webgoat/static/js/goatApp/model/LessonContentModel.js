define([
        'jquery',
        'underscore',
        'backbone',
        'goatApp/model/HTMLContentModel'
    ],
    function ($,
              _,
              Backbone,
              HTMLContentModel) {

        // Precompile safe, bounded regular expressions to avoid inefficient patterns
        // Matches URLs ending with ".lesson"
        var LESSON_URL_PATTERN = /\.lesson(?:\/.*)?$/;
        // Matches page numbers of 1–4 digits at end of URL, after ".lesson/"
        var LESSON_PAGE_PATTERN = /\.lesson\/(\d{1,4})$/;

        return HTMLContentModel.extend({
            urlRoot: null,
            defaults: {
                items: null,
                selectedItem: null
            },

            initialize: function (options) {

            },

            loadData: function (options) {
                // Ensure lesson name is safely encoded for use in URLs
                var safeName = encodeURIComponent(options.name || '');
                this.urlRoot = safeName + '.lesson';

                var self = this;
                this.fetch().done(function (data) {
                    self.setContent(data);
                });
            },

            setContent: function (content, loadHelps) {
                if (typeof loadHelps === 'undefined') {
                    loadHelps = true;
                }
                this.set('content', content);

                var currentUrl = String(document.URL || '');

                // Safely derive lessonUrl without overly broad or backtracking-prone regex
                if (LESSON_URL_PATTERN.test(currentUrl)) {
                    this.set('lessonUrl', currentUrl.replace(LESSON_URL_PATTERN, '.lesson'));
                } else {
                    this.set('lessonUrl', currentUrl);
                }

                // Safely extract page number if present using a precompiled, bounded regex
                var pageMatch = currentUrl.match(LESSON_PAGE_PATTERN);
                if (pageMatch && pageMatch[1]) {
                    this.set('pageNum', pageMatch[1]);
                } else {
                    this.set('pageNum', 0);
                }

                this.trigger('content:loaded', this, loadHelps);
            },

            fetch: function (options) {
                options = options || {};
                return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html" }, options));
            }
        });
    });
