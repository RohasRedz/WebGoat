// Delta Jest tests for LessonContentModel.js focusing only on the new TITLE_REGEX validation.

const path = require('path');

// In a real project, this path might be resolved differently depending on the bundler/loader.
const modelPath = path.resolve(__dirname, '../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

// Require the model under test
const LessonContentModel = require(modelPath);

describe('LessonContentModel TITLE_REGEX validation (delta test)', () => {

    test('validate() accepts titles containing only allowed characters', () => {
        const model = new LessonContentModel();
        const attrs = {
            title: "Valid_Title 123.-()!,\"'",
            assignments: []
        };

        const error = model.validate(attrs);

        expect(error).toBeUndefined();
    });

    test('validate() rejects titles with disallowed characters due to TITLE_REGEX', () => {
        const model = new LessonContentModel();
        const attrs = {
            title: "Invalid@Title",
            assignments: []
        };

        const error = model.validate(attrs);

        expect(error).toBe('Title contains invalid characters');
    });
});
