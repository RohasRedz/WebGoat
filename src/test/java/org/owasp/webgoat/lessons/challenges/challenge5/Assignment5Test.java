package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.owasp.webgoat.container.assignments.AttackResultFactory.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix:
 * - Before fix: string-concatenated SQL with user-supplied username and password.
 * - After fix:  parameterized PreparedStatement with bound parameters.
 *
 * We verify:
 * - The SQL string no longer contains concatenated user input.
 * - The PreparedStatement parameters are set using setString with expected values.
 */
public class Assignment5Test {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment5;

  @BeforeEach
  void setUp() {
    dataSource = Mockito.mock(LessonDataSource.class);
    flags = Mockito.mock(Flags.class);
    assignment5 = new Assignment5(dataSource, flags);
  }

  @Test
  void login_shouldUseParameterizedQueryAndNotConcatenateUserInput() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "anyPassword";
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    when(connection.prepareStatement(sqlCaptor.capture())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert - SQL text uses placeholders instead of inline user data
    String usedSql = sqlCaptor.getValue();
    assertEquals(
        "select password from challenge_users where userid = ? and password = ?",
        usedSql,
        "SQL should use parameter placeholders and not embed user values directly");

    // Assert - parameters are bound via setString
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // Ensure success path still works with proper binding
    // (we don't deeply validate AttackResult internals here, only that success is returned)
    // The success message content is owned by existing tests; here we only check status/flow.
    org.assertj.core.api.Assertions.assertThat(result.getLessonCompleted()).isTrue();
  }
}
