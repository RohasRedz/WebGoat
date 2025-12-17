// TODO: Adjust package if the real package differs from this guess based on path.
package org.owasp.webgoat.lessons.challenges.challenge5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix.
 *
 * The updated code uses a parameterized query with PreparedStatement:
 *   SELECT * FROM users WHERE username = ?
 *
 * These tests ensure:
 * - User input is passed as a parameter, not concatenated into SQL.
 * - Malicious input does not alter the query structure.
 */
class Assignment5Test {

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private Assignment5 assignment5;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = Mockito.mock(DataSource.class);
        connection = Mockito.mock(Connection.class);
        preparedStatement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("data_column")).thenReturn("some-data");

        assignment5 = new Assignment5(dataSource);
    }

    @Test
    void getUserDataShouldUseParameterizedQueryWithPreparedStatement() throws Exception {
        // Arrange
        String username = "alice";

        // Act
        String result = assignment5.getUserData(username);

        // Assert
        // Ensure SQL text uses a placeholder and is not constructed with concatenated user input.
        verify(connection).prepareStatement("SELECT * FROM users WHERE username = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(resultSet).getString("data_column");
        assertThat(result).isEqualTo("some-data");
    }

    @Test
    void getUserDataShouldNotAllowSqlInjectionViaUsername() throws Exception {
        // Arrange
        String maliciousUsername = "alice' OR '1'='1";

        // Act
        assignment5.getUserData(maliciousUsername);

        // Assert
        // The SQL string used in prepareStatement must remain the same, regardless of malicious input.
        verify(connection).prepareStatement("SELECT * FROM users WHERE username = ?");
        // Malicious input is bound as a parameter; it must not be part of the SQL text.
        verify(preparedStatement).setString(1, maliciousUsername);
        verify(preparedStatement).executeQuery();

        // Ensure we never pass a SQL string that already contains the malicious payload.
        verify(connection, never()).prepareStatement(contains(maliciousUsername));
    }

    @Test
    void getUserDataShouldHandleNoResultGracefully() throws Exception {
        // Arrange
        String username = "bob";
        when(resultSet.next()).thenReturn(false);

        // Act
        String result = assignment5.getUserData(username);

        // Assert
        assertThat(result).isNull();
        verify(connection).prepareStatement("SELECT * FROM users WHERE username = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void getUserDataShouldCloseResources() throws Exception {
        // Arrange
        String username = "charlie";

        // Act
        assignment5.getUserData(username);

        // Assert
        // Since try-with-resources is used, close() should be invoked on the resources.
        verify(preparedStatement).close();
        verify(resultSet).close();
        verify(connection).close();
    }
}
