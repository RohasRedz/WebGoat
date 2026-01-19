package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    @Test
    @DisplayName("login should use parameterized PreparedStatement instead of concatenated SQL with user inputs")
    void login_usesPreparedStatementParameters() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("dummy-flag");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);
        String username = "Larry";
        String password = "anyPassword' OR '1'='1";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        // ensure preparedStatement is created with parameter placeholders, not concatenated user input
        verify(connection).prepareStatement(sqlCaptor.capture());
        String usedSql = sqlCaptor.getValue();
        assertEquals("select password from challenge_users where userid = ? and password = ?", usedSql);

        // verify parameter binding order and values
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // ensure query executed
        verify(preparedStatement).executeQuery();

        // success path still works
        // (we don't assert message content here, only that it is a success)
        // AttackResult doesn't expose direct getters; rely on toString containing "success"
        // or use equals via the builder contract; here we just ensure no exception and non-null.
        org.junit.jupiter.api.Assertions.assertNotNull(result);
    }
}
