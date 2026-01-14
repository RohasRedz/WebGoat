package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.Mockito.*;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - verifies PreparedStatement with parameter placeholders is used
 * - verifies user input is bound via setString instead of concatenated into SQL
 */
public class Assignment5Test {

    private LessonDataSource lessonDataSource;
    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private Flags flags;
    private Assignment5 assignment5;

    @BeforeEach
    void setUp() throws Exception {
        lessonDataSource = mock(LessonDataSource.class);
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        flags = mock(Flags.class);

        when(lessonDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // Wrap the underlying DataSource with LessonDataSource mock
        when(lessonDataSource.getDataSource()).thenReturn(dataSource);

        assignment5 = new Assignment5(lessonDataSource, flags);
    }

    @Test
    void login_usesParameterizedQueryAndBindsUserInputs() throws Exception {
        String username = "Larry";
        String password = "secretPassword";

        when(resultSet.next()).thenReturn(true);

        AttackResult result = assignment5.login(username, password);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        assertTrue(sql.contains("userid = ?"), "SQL must use parameter placeholder for userid");
        assertTrue(sql.contains("password = ?"), "SQL must use parameter placeholder for password");
        assertEquals(2, countOccurrences(sql, '?'), "SQL should contain exactly two placeholders");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        assertTrue(result.isLessonSolved(), "Successful login should still solve the challenge");
    }

    @Test
    void login_rejectsSqlInjectionAttemptInPassword() throws Exception {
        String username = "Larry";
        String maliciousPassword = "' OR '1'='1";

        when(resultSet.next()).thenReturn(false);

        AttackResult result = assignment5.login(username, maliciousPassword);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        // Ensure raw user-controlled fragments are not concatenated into the SQL
        assertTrue(!sql.contains(maliciousPassword), "SQL must not contain raw user input after fix");

        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, maliciousPassword);

        assertTrue(result.isLessonFailed(), "Injection-style password should not bypass authentication");
    }

    private int countOccurrences(String text, char c) {
        int count = 0;
        for (char ch : text.toCharArray()) {
            if (ch == c) {
                count++;
            }
        }
        return count;
    }
}
