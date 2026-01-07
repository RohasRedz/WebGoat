package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    // Helper to create a mocked LessonDataSource with a fully mocked JDBC chain
    private LessonDataSource createMockLessonDataSource(ResultSet resultSet) throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(resultSet);

        DataSource mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);

        LessonDataSource lessonDataSource = mock(LessonDataSource.class);
        when(lessonDataSource.getConnection()).thenReturn(mockConnection);

        return lessonDataSource;
    }

    private Flags createMockFlags() {
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");
        return flags;
    }

    @Test
    @DisplayName("login should authenticate successfully with correct credentials using parameterized query")
    void login_success_withCorrectCredentials() throws Exception {
        // Arrange
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockResultSet.next()).thenReturn(true); // simulate found record

        LessonDataSource lessonDataSource = createMockLessonDataSource(mockResultSet);
        Flags flags = createMockFlags();

        Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "correct_password");

        // Assert
        // We only assert success; we don't introspect SQL string, ensuring behavior focus
        assertEquals(true, result.getLessonCompleted(), "Expected login to succeed for valid credentials");
    }

    @Test
    @DisplayName("login should fail for SQL injection payload and not bypass authentication")
    void login_fails_forSqlInjectionPayload() throws Exception {
        // Arrange
        ResultSet mockResultSet = mock(ResultSet.class);
        // No rows returned, even if payload tries to tamper with query
        when(mockResultSet.next()).thenReturn(false);

        LessonDataSource lessonDataSource = createMockLessonDataSource(mockResultSet);
        Flags flags = createMockFlags();

        Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

        String injectionPassword = "' OR '1'='1"; // typical SQL injection payload

        // Act
        AttackResult result = assignment5.login("Larry", injectionPassword);

        // Assert
        assertEquals(false, result.getLessonCompleted(), "SQL injection payload must not bypass authentication");
    }
}
