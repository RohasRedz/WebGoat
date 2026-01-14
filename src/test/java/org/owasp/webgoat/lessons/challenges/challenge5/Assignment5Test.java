package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5, focused on the SQL injection fix:
 * verifying that a PreparedStatement with parameter placeholders is used
 * and that user input containing quotes is safely bound via setString.
 */
public class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;

  private Assignment5 assignment5;

  @BeforeEach
  void setUp() {
    dataSource = mock(LessonDataSource.class);
    flags = mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);
  }

  @Test
  void login_usesPreparedStatementWithBoundParameters_andHandlesQuotedInputSafely() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "p' OR '1'='1";

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    Mockito.when(dataSource.getConnection()).thenReturn(connection);
    Mockito
        .when(connection.prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?")))
        .thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
    Mockito.when(resultSet.next()).thenReturn(true);
    Mockito.when(flags.getFlag(5)).thenReturn("flag-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // Verify that parameters are set via setString instead of concatenation into the SQL string.
    verify(preparedStatement, times(1)).setString(1, username);
    verify(preparedStatement, times(1)).setString(2, password);
    verify(preparedStatement, times(1)).executeQuery();

    // Ensure the happy-path behavior is preserved when credentials are correct.
    assertEquals("success", result.getType().name().toLowerCase());
  }

  @Test
  void login_failsWhenUsernameIsNotLarry_evenWithSpecialCharacters() throws Exception {
    // Arrange
    String username = "Mallory' OR '1'='1";
    String password = "anything";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // This ensures the precondition check on username is still enforced and not bypassed by injection.
    assertEquals("failure", result.getType().name().toLowerCase());
  }
}
