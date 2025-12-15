const LessonContentModel = require('../../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel');

// Minimal Backbone/HTMLContentModel stubbing is assumed to be handled by the Jest environment or test harness.
// These tests focus on the behavior of setContent with respect to lessonUrl and pageNum only.

describe('LessonContentModel setContent URL and pageNum behavior', () => {
    let originalDocumentUrl;

    beforeAll(() => {
        originalDocumentUrl = global.document && global.document.URL;
        if (!global.document) {
            global.document = {};
        }
    });

    afterAll(() => {
        if (originalDocumentUrl !== undefined) {
            global.document.URL = originalDocumentUrl;
        }
    });

    function createModelInstance() {
        // In the real application, LessonContentModel extends a Backbone model. For these delta tests, we
        // provide a minimal stub that supports set/get and trigger so we can verify URL and pageNum behavior.
        const model = new LessonContentModel();
        // Ensure attributes object exists for direct inspection if Backbone is not available.
        if (!model.attributes) {
            model.attributes = {};
        }
        const originalSet = model.set ? model.set.bind(model) : (key, value) => { model.attributes[key] = value; };
        model.set = (key, value) => {
            // Support both key/value and object signatures lightly if Backbone is present.
            if (typeof key === 'object') {
                Object.keys(key).forEach(k => {
                    model.attributes[k] = key[k];
                });
            } else {
                model.attributes[key] = value;
            }
            return originalSet(key, value);
        };
        if (!model.get) {
            model.get = (key) => model.attributes[key];
        }
        if (!model.trigger) {
            model.trigger = jest.fn();
        }
        return model;
    }

    test('URL ending with .lesson/12 sets lessonUrl and pageNum correctly', () => {
        const lessonUrl = 'http://host/path/topic.lesson/12';
        global.document.URL = lessonUrl;

        const model = createModelInstance();
        model.setContent('<html></html>', true);

        expect(model.get('lessonUrl')).toMatch(/topic\.lesson$/);
        expect(model.get('pageNum')).toBe('12');
    });

    test('URL ending with .lesson/12?x=1 sets lessonUrl, pageNum falls back to 0', () => {
        const lessonUrl = 'http://host/path/topic.lesson/12?x=1';
        global.document.URL = lessonUrl;

        const model = createModelInstance();
        model.setContent('<html></html>', true);

        expect(model.get('lessonUrl')).toMatch(/topic\.lesson$/);
        expect(model.get('pageNum')).toBe(0);
    });

    test('URL ending with .lesson without page number sets lessonUrl and pageNum 0', () => {
        const lessonUrl = 'http://host/path/topic.lesson';
        global.document.URL = lessonUrl;

        const model = createModelInstance();
        model.setContent('<html></html>', true);

        expect(model.get('lessonUrl')).toMatch(/topic\.lesson$/);
        expect(model.get('pageNum')).toBe(0);
    });
});
