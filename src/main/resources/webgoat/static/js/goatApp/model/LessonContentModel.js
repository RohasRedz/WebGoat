define(['jquery', 'underscore', 'backbone'], function ($, _, Backbone) {

    // Simple, efficient, and safe regular expression for titles:
    // - Allows letters, digits, spaces, and basic punctuation.
    // - No nested quantifiers or ambiguous patterns (avoids ReDoS).
    var TITLE_REGEX = /^[A-Za-z0-9 _.,!'"\-()]+$/;

    var LessonContentModel = Backbone.Model.extend({

        defaults: {
            title: '',
            introduction: '',
            assignments: [],
            showNextButton: false
        },

        validate: function (attrs) {
            if (typeof attrs.title !== 'string' || attrs.title.length === 0) {
                return 'Title is required';
            }

            // New: Efficient, safe regex-based validation for title
            if (!TITLE_REGEX.test(attrs.title)) {
                return 'Title contains invalid characters';
            }

            if (!_.isArray(attrs.assignments)) {
                return 'Assignments must be an array';
            }
            return undefined;
        }
    });

    return LessonContentModel;
});
