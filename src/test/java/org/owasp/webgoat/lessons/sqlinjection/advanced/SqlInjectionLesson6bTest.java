package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

    // Helper subclass to stub getPassword() without changing production code
    private static class SqlInjectionLesson6bStub extends SqlInjectionLesson6b {

        private final String passwordToReturn;

        SqlInjectionLesson6bStub(LessonDataSource dataSource, String passwordToReturn) {
            super(dataSource);
            this.passwordToReturn = passwordToReturn;
        }

        @Override
        protected String getPassword() {
            return passwordToReturn;
        }
    }

    @Test
    @DisplayName("completed should fail when userid_6b does not equal getPassword()")
    void completed_returnsFailureWhenUserIdDoesNotMatchPassword() throws IOException {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6bStub(dataSource, "expectedPassword");

        AttackResult result = lesson.completed("wrongPassword");

        assertFalse(result.isLessonSolved(), "Mismatched userid_6b should not solve the lesson");
    }

    @Test
    @DisplayName("completed should succeed when userid_6b equals getPassword()")
    void completed_returnsSuccessWhenUserIdMatchesPassword() throws IOException {
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6bStub(dataSource, "expectedPassword");

        AttackResult result = lesson.completed("expectedPassword");

        assertTrue(result.isLessonSolved(), "Matching userid_6b should solve the lesson");
    }
}
