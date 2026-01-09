package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing only on the SQL injection fix:
 * - Query must use PreparedStatement with parameter placeholders.
 * - User-supplied values must be passed via setString, not concatenation.
 */
class Assignment5Test {

  @Test
  void login_usesPreparedStatementParametersForUserInput() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment = new Assignment5(dataSource, flags);

    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    String username = "Larry";
    String password = "safePassword";

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    assertThat(sql)
        .as("SQL must use positional parameters, not string concatenation with user input")
        .contains("userid = ?")
        .contains("password = ?")
        .doesNotContain(username)
        .doesNotContain(password);

    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
    verify(preparedStatement).executeQuery();

    assertThat(result.getLessonCompleted()).isTrue();
  }

  @Test
  void login_rejectsNonLarryUserEvenWithValidPassword() throws Exception {
    // Arrange
    LessonDataSource dataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment = new Assignment5(dataSource, flags);

    String username = "Mallory";
    String password = "anything";

    // Act
    AttackResult result = assignment.login(username, password);

    // Assert
    // This ensures that even if SQL injection were attempted, the precondition still blocks it.
    assertThat(result.getLessonCompleted()).isFalse();
    verifyNoInteractions(dataSource);
  }
}
