package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

/**
 * Negative delta test ensuring that SQL injection attempts are not effective
 * after switching to a parameterized PreparedStatement.
 */
class Assignment5SqlInjectionDeltaTest {

  @Test
  @DisplayName("SQL injection via username should fail when using parameterized query")
  void loginShouldNotAllowSqlInjectionViaUsername() throws Exception {
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
    // Injection should not find a row when treated as data
    when(resultSet.next()).thenReturn(false);

    String maliciousUsername = "Larry' OR '1'='1";
    String password = "anything";

    // Act
    AttackResult result = assignment5.login(maliciousUsername, password);

    // Assert
    verify(connection)
        .prepareStatement("select password from challenge_users where userid = ? and password = ?");
    verify(preparedStatement).setString(1, maliciousUsername);
    verify(preparedStatement).setString(2, password);

    assertThat(result.getLessonCompleted())
        .as("SQL injection via username must not bypass authentication when using parameters")
        .isFalse();
  }
}
