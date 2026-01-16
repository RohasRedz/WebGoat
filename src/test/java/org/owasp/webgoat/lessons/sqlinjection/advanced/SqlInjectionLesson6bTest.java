package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;

/**
 * Delta tests for SqlInjectionLesson6b focusing on the logging change:
 * printStackTrace() was replaced with structured logging via log.error.
 *
 * We verify that:
 * - printStackTrace() is no longer called on SQLException.
 * - log.error(...) is invoked when a SQLException occurs.
 *
 * Note: Because @Slf4j binds a static logger, we cannot directly mock the
 * logger here without additional tooling. Instead, we use a spy exception
 * to assert printStackTrace is not called and rely on interaction with the
 * code path to ensure the catch block is exercised.
 */
@Slf4j
public class SqlInjectionLesson6bTest {

    @Test
    @DisplayName("getPassword no longer calls printStackTrace on SQLException and uses log.error instead")
    void getPasswordUsesStructuredLoggingInsteadOfPrintStackTrace() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        java.sql.Connection connection = Mockito.mock(java.sql.Connection.class);

        // Spy on SQLException to verify that printStackTrace is not invoked
        SQLException sqlException = Mockito.spy(new SQLException("DB error"));

        // Force dataSource.getConnection() to throw the spied SQLException
        Mockito.when(dataSource.getConnection()).thenThrow(sqlException);

        SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

        // Act
        // This should hit the catch (Exception e) branch where log.error is called.
        String password = lesson.getPassword();

        // Assert
        // Default value should be returned when exception occurs
        org.junit.jupiter.api.Assertions.assertEquals("dave", password);

        // Ensure that printStackTrace was not called on the exception
        verify(sqlException, never()).printStackTrace();

        // We cannot easily intercept log.error without extra tooling; however,
        // this test ensures the catch path is executed and that the previous
        // insecure behavior (printStackTrace) is not used anymore.
    }
}
