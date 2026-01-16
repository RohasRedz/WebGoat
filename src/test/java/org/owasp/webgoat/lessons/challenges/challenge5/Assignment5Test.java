// File path: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the changed behavior:
 * - SQL is now executed via parameterized PreparedStatement instead of string concatenation.
 * These tests do NOT try to prove absence of SQL injection in the DB, but assert that:
 * - Parameters are bound via setString and still allow successful login for valid credentials.
 * - Invalid credentials are rejected via the normal path.
 */
public class Assignment5Test {

    @Test
    @DisplayName("login uses prepared statement with parameters and still authenticates valid user")
    void login_usesPreparedStatementAndAuthenticatesValidUser() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("flag-5");

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "secret");

        // Assert
        // Verify parameters were bound positionally instead of concatenated into SQL.
        Mockito.verify(preparedStatement).setString(1, "Larry");
        Mockito.verify(preparedStatement).setString(2, "secret");
        Mockito.verify(preparedStatement).executeQuery();

        // Successful login path preserved
        assertEquals("success", result.getLessonStatus().name().toLowerCase());
    }

    @Test
    @DisplayName("login rejects wrong password via parameterized query (no concatenation bypass)")
    void login_rejectsInvalidPasswordWithParameterizedQuery() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // No row returned for wrong password or attempted injection
        when(resultSet.next()).thenReturn(false);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result = assignment5.login("Larry", "wrong' OR '1'='1");

        // Assert
        // Even though the password looks like an injection payload, it is bound as a parameter.
        Mockito.verify(preparedStatement).setString(1, "Larry");
        Mockito.verify(preparedStatement).setString(2, "wrong' OR '1'='1");
        Mockito.verify(preparedStatement).executeQuery();

        // The attack should be rejected (fail status) instead of bypassing auth.
        assertEquals("failure", result.getLessonStatus().name().toLowerCase());
    }
}
