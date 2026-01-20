package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed returns failed result when insecure getPassword is disabled")
    void completed_returnsFailedWhenGetPasswordDisabled() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        AttackResult result = lesson.completed("any-userid");

        // Assert
        // After fix, getPassword throws UnsupportedOperationException and is caught,
        // so completed() must return a failed AttackResult instead of exposing details.
        assertEquals("failed", result.getLessonStatus().name().toLowerCase());
    }

    @Test
    @DisplayName("getPassword throws UnsupportedOperationException to prevent insecure use")
    void getPassword_throwsUnsupportedOperationException() {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act & Assert
        UnsupportedOperationException ex =
                assertThrows(UnsupportedOperationException.class, lesson::getPassword);
        assertTrue(ex.getMessage().contains("Secure credential management is required"));
    }
}
