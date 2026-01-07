// Test file path (mirrors main with 'main' -> 'test'):
// src/test/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionLesson6bTest.java
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

/**
 * Delta tests for SqlInjectionLesson6b focused on preserving
 * completed() behavior after logging changes in getPassword():
 * - When userid_6b equals getPassword(), completed() must succeed.
 * - When userid_6b does not equal getPassword(), completed() must fail.
 *
 * These tests intentionally do not inspect logging side effects.
 */
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("completed succeeds when userid_6b matches getPassword")
    void completed_succeedsWhenUserIdMatchesPassword() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);

        // Use a spy so we can control getPassword() without changing production code
        SqlInjectionLesson6b endpoint = spy(new SqlInjectionLesson6b(dataSource));
        doReturn("secret-pass").when(endpoint).getPassword();

        // Act
        AttackResult result = endpoint.completed("secret-pass");

        // Assert
        assertTrue(result.isSuccess(), "completed() should succeed when userid_6b matches getPassword()");
    }

    @Test
    @DisplayName("completed fails when userid_6b does not match getPassword")
    void completed_failsWhenUserIdDoesNotMatchPassword() throws IOException {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);

        SqlInjectionLesson6b endpoint = spy(new SqlInjectionLesson6b(dataSource));
        doReturn("secret-pass").when(endpoint).getPassword();

        // Act
        AttackResult result = endpoint.completed("wrong-pass");

        // Assert
        assertFalse(result.isSuccess(), "completed() should fail when userid_6b does not match getPassword()");
    }
}
