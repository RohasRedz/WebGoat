package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized query and prevent SQL injection")
    void login_usesPreparedStatementParameters_andPreventsInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
            .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Simulate malicious input that would have succeeded with string concatenation
        String maliciousUsername = "Larry' OR '1'='1";
        String maliciousPassword = "anything";

        // Act
        AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

        // Assert
        verify(connection, times(1))
            .prepareStatement("select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement, times(1)).setString(1, maliciousUsername);
        verify(preparedStatement, times(1)).setString(2, maliciousPassword);
        verify(preparedStatement, times(1)).executeQuery();

        assert result != null;
    }
}
