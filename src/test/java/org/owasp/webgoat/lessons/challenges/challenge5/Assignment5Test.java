package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

/**
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - SQL is executed via parameterized PreparedStatement with setString bindings.
 */
class Assignment5Test {

    @Test
    @DisplayName("login should use prepared statement parameters and resist SQL injection payload")
    void loginShouldUseParameterizedQueryAndNotBeBypassedBySqlInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Craft inputs that would exploit SQL injection if concatenation was still used
        String maliciousUsername = "Larry' OR '1'='1";
        String maliciousPassword = "anything' OR '1'='1";

        // Simulate DB returning no row even if the injection payload is provided,
        // which is the behavior we expect when parameters are used correctly.
        when(resultSet.next()).thenReturn(false);

        // Act
        AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

        // Assert
        // Verify secure parameter binding is used
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, maliciousUsername);
        verify(preparedStatement).setString(2, maliciousPassword);

        // Since no row is found, the attack should not succeed
        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
