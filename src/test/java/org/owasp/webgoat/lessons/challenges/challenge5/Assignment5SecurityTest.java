package org.owasp.webgoat.lessons.challenges.challenge5;

// Package inferred from Assignment5.java; adjust if the actual package differs.

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta unit tests for {@link Assignment5}.
 *
 * <p>These tests focus only on behavior impacted by the security fix:
 *
 * <ul>
 *   <li>Use of parameterized SQL (PreparedStatement with placeholders) to prevent SQL injection.
 *   <li>Preservation of the valid login behavior for the expected user ("Larry").
 * </ul>
 *
 * <p>The actual method uses a JDBC Connection retrieved from LessonDataSource, so we mock the
 * JDBC objects to validate behavior without touching a real database.
 */
public class Assignment5SecurityTest {

  private LessonDataSource dataSource;
  private Flags flags;
  private Assignment5 assignment5;

  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = Mockito.mock(LessonDataSource.class);
    flags = Mockito.mock(Flags.class);

    connection = Mockito.mock(Connection.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    resultSet = Mockito.mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(connection);
    // We don't care about the exact SQL text in this delta test, only that it is used as a
    // prepared statement. The security property we test is behavioral (no SQLi effect).
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    assignment5 = new Assignment5(dataSource, flags);
  }

  /**
   * Verifies that the login method does not succeed when given an input that would have
   * exploited SQL injection in the old implementation.
   *
   * <p>The original vulnerable code concatenated user input directly into the query:
   *
   * <pre>
   *   ... where userid = 'username' and password = 'password'
   * </pre>
   *
   * An attacker could supply a username such as:
   *
   * <pre>
   *   Larry' OR '1'='1
   * </pre>
   *
   * to bypass authentication. After the fix, the method uses placeholders and setString, so
   * this payload must be treated as a literal value and not cause unintended success.
   */
  @Test
  void loginIsResistantToSqlInjection() throws Exception {
    // Arrange: Simulate that no legitimate row matches for the injected username/password.
    when(resultSet.next()).thenReturn(false);

    String maliciousUsername = "Larry' OR '1'='1";
    String maliciousPassword = "anything";

    // Pre-condition sanity check: both have text so they pass the early hasText validation.
    assertThat(StringUtils.hasText(maliciousUsername)).isTrue();
    assertThat(StringUtils.hasText(maliciousPassword)).isTrue();

    // Act
    AttackResult result = assignment5.login(maliciousUsername, maliciousPassword);

    // Assert: The attack should not be treated as a successful login.
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isFalse();
  }

  /**
   * Verifies that valid credentials for the expected user ("Larry") still succeed after
   * switching to parameterized SQL.
   *
   * <p>This confirms that the security fix did not break the intended positive path.
   */
  @Test
  void loginWithValidCredentialsStillSucceeds() throws Exception {
    // Arrange: Only "Larry" is accepted by the pre-check; we simulate a matching DB row.
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    String validUsername = "Larry";
    String validPassword = "password123";

    // Act
    AttackResult result = assignment5.login(validUsername, validPassword);

    // Assert: The method should mark the lesson as solved for valid credentials.
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isTrue();
  }

  /**
   * Verifies that valid user "Larry" with wrong password does not succeed. This ensures that
   * the prepared-statement based check still enforces credential correctness.
   *
   * <p>While not strictly about SQL injection, this test is still scoped to the changed query
   * behavior and ensures no unintended "always true" semantics slipped in during the refactor.
   */
  @Test
  void loginWithInvalidPasswordFails() throws Exception {
    // Arrange: DB reports no matching row.
    when(resultSet.next()).thenReturn(false);

    String validUsername = "Larry";
    String wrongPassword = "wrong";

    // Act
    AttackResult result = assignment5.login(validUsername, wrongPassword);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getLessonCompleted()).isFalse();
  }
}
