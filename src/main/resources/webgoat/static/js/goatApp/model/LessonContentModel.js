define(
    ['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'],
    function ($, _, Backbone, HTMLContentModel) {
        return HTMLContentModel.extend({
            urlRoot: null,
            defaults: {
                items: null,
                selectedItem: null
            },

            initialize: function (options) {},

            loadData: function (options) {
                this.urlRoot =
                    _.escape(encodeURIComponent(options.name)) + '.lesson';
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

                var currentUrl = document.URL;

                // Derive base .lesson URL without relying on catastrophic backtracking
                var lessonBaseIndex = currentUrl.indexOf('.lesson');
                if (lessonBaseIndex !== -1) {
                    this.set(
                        'lessonUrl',
                        currentUrl.substring(0, lessonBaseIndex + '.lesson'.length)
                    );
                } else {
                    this.set('lessonUrl', currentUrl);
                }

                // Extract pageNum using safe regex on the substring after ".lesson/"
                var pageNum = 0;
                if (lessonBaseIndex !== -1) {
                    var afterLesson = currentUrl.substring(
                        lessonBaseIndex + '.lesson'.length
                    );
                    // Expect format like "/<digits>" or nothing
                    var pageMatch = /^\/(\d{1,4})$/.exec(afterLesson);
                    if (pageMatch) {
                        pageNum = parseInt(pageMatch[1], 10);
                    }
                }
                this.set('pageNum', pageNum);

                this.trigger('content:loaded', this, loadHelps);
            },

            fetch: function (options) {
                options = options || {};
                return Backbone.Model.prototype.fetch.call(
                    this,
                    _.extend({ dataType: 'html' }, options)
                );
            }
        });
    }
);
