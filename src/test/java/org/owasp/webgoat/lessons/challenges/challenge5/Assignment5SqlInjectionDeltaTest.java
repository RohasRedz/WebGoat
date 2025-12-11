package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for Assignment5 focusing only on the SQL injection fix.
 *
 * Original vulnerability:
 * - SQL query was built by directly concatenating username_login and password_login into
 *   the SQL string, enabling SQL injection.
 *
 * Fixed behavior to verify:
 * - The PreparedStatement uses parameter placeholders (`?`) instead of inlined values.
 * - User inputs (username_login, password_login) are passed via `setString(...)`
 *   and are not concatenated into the SQL string.
 */
@ExtendWith(MockitoExtension.class)
class Assignment5SqlInjectionDeltaTest {

  @Mock
  private LessonDataSource dataSource;

  @Mock
  private Flags flags;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement preparedStatement;

  @Mock
  private ResultSet resultSet;

  // Class under test. @RequiredArgsConstructor uses dataSource and flags.
  @InjectMocks
  private Assignment5 assignment5;

  @Test
  @DisplayName("login() should use a parameterized PreparedStatement with placeholders for userid and password")
  void loginShouldUseParameterizedPreparedStatement() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "s3cret";

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // 1) Verify the SQL passed into prepareStatement.
    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(connection).prepareStatement(sqlCaptor.capture());
    String sqlUsed = sqlCaptor.getValue();

    // The fixed code must use ? placeholders instead of concatenated username/password.
    assertThat(sqlUsed)
        .as("SQL must use parameter placeholders for userid and password")
        .contains("where userid = ? and password = ?");

    // Ensure the SQL itself does not directly contain the user-supplied values.
    assertThat(sqlUsed)
        .as("SQL string must not inline user-supplied username")
        .doesNotContain(username);
    assertThat(sqlUsed)
        .as("SQL string must not inline user-supplied password")
        .doesNotContain(password);

    // 2) Verify that parameters are bound via setString rather than being concatenated.
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);

    // 3) Sanity check that the flow is still successful when credentials match.
    assertThat(result.getLessonCompleted())
        .as("Challenge must still be solvable with correct credentials")
        .isTrue();
  }

  @Test
  @DisplayName("login() should not perform any DB query when username or password is blank (guard unchanged by fix)")
  void loginShouldShortCircuitOnBlankInputsWithoutTouchingDatabase() throws Exception {
    // NOTE:
    // This delta test ensures that the early-return guard (which existed before the fix)
    // is still in place and that no SQL is executed when inputs are empty. This indirectly
    // confirms that the moved/changed SQL logic has not affected the guard behavior.

    // Arrange
    String emptyUsername = "";
    String emptyPassword = "   "; // whitespace-only

    // Act
    AttackResult result1 = assignment5.login(emptyUsername, "somePass");
    AttackResult result2 = assignment5.login("Larry", emptyPassword);

    // Assert
    assertThat(result1.getLessonCompleted())
        .as("Login with empty username must fail without DB interaction")
        .isFalse();
    assertThat(result2.getLessonCompleted())
        .as("Login with empty password must fail without DB interaction")
        .isFalse();

    // No DB connections should be acquired when input is invalid.
    verify(dataSource, never()).getConnection();
  }

  @Test
  @DisplayName("login() should still enforce that only user 'Larry' can proceed to DB check")
  void loginShouldStillRejectNonLarryUserBeforeDatabaseInteraction() throws Exception {
    // NOTE:
    // This delta test protects against regressions where moving SQL code might accidentally
    // bypass the 'Larry' check. It focuses on behavior directly around the changed SQL area.

    // Arrange
    String notLarry = "Eve";
    String password = "anything";

    // Act
    AttackResult result = assignment5.login(notLarry, password);

    // Assert
    assertThat(result.getLessonCompleted())
        .as("Non-'Larry' users must still be rejected before DB query")
        .isFalse();

    // Verify that the database is not touched for non-'Larry' users.
    verify(dataSource, never()).getConnection();
  }
}
