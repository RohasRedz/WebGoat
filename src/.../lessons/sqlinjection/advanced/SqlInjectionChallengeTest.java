// Delta_UnitTest_Agent
// Package inferred from source file path; adjust if actual package differs.
// TODO: Confirm this package matches the actual SqlInjectionChallenge.java package.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Delta tests for SqlInjectionChallenge focusing on the SQL injection fix.
 *
 * Original (vulnerable) behavior (conceptual):
 *   - Constructed SQL by concatenating productId into the SQL string.
 *
 * Updated (secure) behavior:
 *   - Uses a parameterized query with PreparedStatement:
 *       "SELECT name, description FROM products WHERE id = ?"
 *   - Binds productId via setString(1, productId).
 *
 * These tests ensure:
 * - The query text passed to prepareStatement is constant and uses a placeholder.
 * - User-controlled productId is bound as a parameter, not concatenated.
 * - Malicious productId does not alter the SQL structure.
 */
class SqlInjectionChallengeTest {

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private SqlInjectionChallenge challenge;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("name")).thenReturn("Widget");
        when(resultSet.getString("description")).thenReturn("A thing");

        challenge = new SqlInjectionChallenge(dataSource);
    }

    @Test
    @DisplayName("Should use parameterized query with PreparedStatement")
    void shouldUseParameterizedQueryWithPreparedStatement() throws Exception {
        // Arrange
        String productId = "42";

        // Act
        String result = challenge.getProductDetails(productId);

        // Assert
        InOrder inOrder = inOrder(connection, preparedStatement, resultSet);
        inOrder.verify(connection).prepareStatement("SELECT name, description FROM products WHERE id = ?");
        inOrder.verify(preparedStatement).setString(1, productId);
        inOrder.verify(preparedStatement).executeQuery();
        inOrder.verify(resultSet).next();

        assertThat(result).isEqualTo("Widget - A thing");
    }

    @Test
    @DisplayName("Should not concatenate malicious productId into SQL text")
    void shouldNotConcatenateMaliciousProductIdIntoSql() throws Exception {
        // Arrange
        String maliciousProductId = "42' OR '1'='1";

        // Act
        challenge.getProductDetails(maliciousProductId);

        // Assert
        // SQL passed to prepareStatement must be static and not contain the malicious payload
        verify(connection).prepareStatement("SELECT name, description FROM products WHERE id = ?");
        verify(preparedStatement).setString(1, maliciousProductId);
        verify(preparedStatement).executeQuery();

        // Ensure we never supply a SQL string that already includes the payload
        verify(connection, never()).prepareStatement(contains(maliciousProductId));
    }

    @Test
    @DisplayName("Should return null when no product is found")
    void shouldReturnNullWhenNoResult() throws SQLException {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        String productId = "999";

        // Act
        String result = challenge.getProductDetails(productId);

        // Assert
        assertThat(result).isNull();
        verify(connection).prepareStatement("SELECT name, description FROM products WHERE id = ?");
        verify(preparedStatement).setString(1, productId);
        verify(preparedStatement).executeQuery();
    }
}
