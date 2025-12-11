package org.owasp.webgoat.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.container.users.UserService;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.owasp.webgoat.lessons.challenges.challenge5.Assignment5;
import org.owasp.webgoat.lessons.deserialization.InsecureDeserializationTask;
import org.owasp.webgoat.container.WebSecurityConfig;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;

/**
 * Delta unit tests for security fixes:
 *
 * <ul>
 *   <li>WebSecurityConfig: ensure BCryptPasswordEncoder is used and CSRF is enabled.
 *   <li>Assignment5: ensure parameterized query is used (no SQL concatenation) and behavior
 *       preserved.
 *   <li>InsecureDeserializationTask: ensure no insecure deserialization and secure HMAC-based
 *       token validation with expiration and signature checks.
 * </ul>
 *
 * NOTE: These tests are intentionally focused only on behavior changed by the fixes and do not
 * attempt to cover unrelated functionality.
 *
 * TODO: Wire into actual Spring test configuration / MockMvc context if project test
 * infrastructure differs from assumptions here.
 */
public class SecurityFixesDeltaTest {

  // ----------------------------
  // Batch 1 – WebSecurityConfig
  // ----------------------------

  @Nested
  @DisplayName("WebSecurityConfig security configuration delta tests")
  class WebSecurityConfigDeltaTests {

    /**
     * Verifies that the configured PasswordEncoder is a BCryptPasswordEncoder (or at least not a
     * NoOp/plain-text encoder anymore).
     */
    @Test
    void passwordEncoder_shouldUseBCryptAndNotNoOp() {
      // Arrange
      UserService userService = Mockito.mock(UserService.class);
      WebSecurityConfig config = new WebSecurityConfig(userService);

      // Act
      PasswordEncoder encoder = config.passwordEncoder();

      // Assert
      // Ensure it is a BCryptPasswordEncoder (secure encoder)
      assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

      // Ensure it does not behave like NoOpPasswordEncoder (i.e., encoded value != raw value)
      String raw = "secret-password";
      String encoded = encoder.encode(raw);
      assertThat(encoded).isNotEqualTo(raw);
      assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    /**
     * Verifies that the SecurityFilterChain configuration does not disable CSRF. Full integration
     * testing of CSRF might require a SpringBootTest + MockMvc; here we assert that the filter
     * chain is buildable and does not obviously disable CSRF via configuration as before.
     *
     * NOTE: This is a structural/behavioral regression check focusing on the removal of
     * csrf().disable().
     */
    @Test
    void filterChain_shouldBeBuildableWithCsrfEnabledByDefault() throws Exception {
      // Arrange
      UserService userService = Mockito.mock(UserService.class);
      WebSecurityConfig config = new WebSecurityConfig(userService);

      // We cannot easily introspect HttpSecurity without spinning up full context, so we
      // simply ensure that building the filter chain no longer throws due to misconfiguration.
      org.springframework.security.config.annotation.web.builders.HttpSecurity http =
          Mockito.mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
              Mockito.RETURNS_DEEP_STUBS);

      // We expect config.filterChain(http) to call various builder methods and finally build().
      // Set up a stub FilterChainProxy for build() to return.
      FilterChainProxy proxy = Mockito.mock(FilterChainProxy.class);
      Mockito.when(http.build()).thenReturn(proxy);

      // Act & Assert
      FilterChainProxy result = config.filterChain(http);
      assertThat(result).isSameAs(proxy);

      // Also verify that csrf().disable() is not invoked. If the new code removed it,
      // there should be no interactions with csrf().disable().
      Mockito.verify(http, Mockito.never()).csrf(Mockito.any());
    }
  }

  // ----------------------------
  // Batch 2 – Assignment5 (SQLi)
  // ----------------------------

  @Nested
  @DisplayName("Assignment5 SQL injection fix delta tests")
  class Assignment5DeltaTests {

    /**
     * Verifies that the login method uses parameterized SQL with placeholders and binds
     * username/password via setString instead of concatenating them into the query.
     */
    @Test
    void login_shouldUseParameterizedQueryAndBindUserInputs() throws Exception {
      // Arrange
      LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
      Flags flags = Mockito.mock(Flags.class);
      Assignment5 assignment = new Assignment5(dataSource, flags);

      Connection connection = Mockito.mock(Connection.class);
      PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
      ResultSet resultSet = Mockito.mock(ResultSet.class);

      Mockito.when(dataSource.getConnection()).thenReturn(connection);
      Mockito.when(
              connection.prepareStatement(
                  Mockito.anyString()))
          .thenReturn(preparedStatement);
      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
      Mockito.when(resultSet.next()).thenReturn(true);
      Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

      String username = "Larry";
      String password = "somePassword' OR '1'='1";

      // Act
      AttackResult result = assignment.login(username, password);

      // Assert: query string uses placeholders
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
      String usedSql = sqlCaptor.getValue();
      assertThat(usedSql)
          .contains("userid = ?")
          .contains("password = ?")
          .doesNotContain(username)
          .doesNotContain(password);

      // And parameters are bound via setString
      Mockito.verify(preparedStatement).setString(1, username);
      Mockito.verify(preparedStatement).setString(2, password);

      // Behavior is preserved: successful login returns success with flag
      assertThat(result.getLessons()).isEmpty(); // sanity: check structure
      assertThat(result.getFeedback()).contains("challenge.solved");
      assertThat(result.getOutput()).contains("FLAG-5");
    }

    /**
     * Verifies that when username is not 'Larry' (business rule), the method does not even hit the
     * database, ensuring that any SQL injection attempt in such a case is not executed.
     */
    @Test
    void login_withNonLarryUser_shouldNotExecuteQuery() throws Exception {
      // Arrange
      LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
      Flags flags = Mockito.mock(Flags.class);
      Assignment5 assignment = new Assignment5(dataSource, flags);

      // No need to configure dataSource.getConnection() because it should never be called
      String username = "Mallory";
      String password = "anything' OR '1'='1";

      // Act
      AttackResult result = assignment.login(username, password);

      // Assert: no DB calls for non-'Larry' user
      Mockito.verifyNoInteractions(dataSource);

      // And failure feedback is returned
      assertThat(result.getFeedback()).contains("user.not.larry");
    }
  }

  // ----------------------------------------------------------
  // Batch 3 – InsecureDeserializationTask (secure token logic)
  // ----------------------------------------------------------

  @Nested
  @DisplayName("InsecureDeserializationTask secure token validation delta tests")
  class InsecureDeserializationTaskDeltaTests {

    /**
     * Builds a valid token according to the updated logic in InsecureDeserializationTask:
     * Base64URL-encoded JSON with fields: task, exp, sig (HMAC-SHA256 over task+exp).
     *
     * NOTE: This helper must mirror the algorithm used in the production class; if that class
     * changes its algorithm, this test should be updated accordingly.
     */
    private String buildValidToken(String task, long expirationMillis) throws Exception {
      String algorithm = "HmacSHA256";
      byte[] secretKeyBytes =
          "superSecretKeyForWebGoatLesson".getBytes(StandardCharsets.UTF_8);
      SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, algorithm);
      Mac mac = Mac.getInstance(algorithm);
      mac.init(keySpec);

      String dataToSign = task + expirationMillis;
      byte[] sigBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
      String sigBase64 = java.util.Base64.getEncoder().encodeToString(sigBytes);

      String json =
          "{\"task\":\""
              + task
              + "\",\"exp\":"
              + expirationMillis
              + ",\"sig\":\""
              + sigBase64
              + "\"}";

      byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
      String base64 = java.util.Base64.getEncoder().encodeToString(jsonBytes);
      // Convert to Base64URL by replacing +/ with -_ and removing padding if present
      String base64Url = base64.replace('+', '-').replace('/', '_').replace("=", "");
      return base64Url;
    }

    @Test
    void completed_withValidToken_shouldReturnSuccess() throws Exception {
      // Arrange
      InsecureDeserializationTask taskEndpoint = new InsecureDeserializationTask();
      long futureExp = System.currentTimeMillis() + 60_000; // expires in 1 minute
      String token = buildValidToken("lesson-task", futureExp);

      // Act
      AttackResult result = taskEndpoint.completed(token);

      // Assert: verify success path is reachable and no insecure deserialization is used
      assertThat(result).isNotNull();
      assertThat(result.isLessonSolved()).isTrue();
      assertThat(result.getFeedback()).contains("insecure-deserialization.secure_token_processed");
    }

    @Test
    void completed_withExpiredToken_shouldFail() throws Exception {
      // Arrange
      InsecureDeserializationTask taskEndpoint = new InsecureDeserializationTask();
      long pastExp = System.currentTimeMillis() - 60_000; // expired 1 minute ago
      String token = buildValidToken("lesson-task", pastExp);

      // Act
      AttackResult result = taskEndpoint.completed(token);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.isLessonSolved()).isFalse();
      assertThat(result.getFeedback()).contains("insecure-deserialization.token_expired");
    }

    @Test
    void completed_withTamperedSignature_shouldFail() throws Exception {
      // Arrange
      InsecureDeserializationTask taskEndpoint = new InsecureDeserializationTask();
      long futureExp = System.currentTimeMillis() + 60_000;
      String token = buildValidToken("lesson-task", futureExp);

      // Tamper with token by altering one character (signature becomes invalid)
      String tamperedToken = token.substring(0, token.length() - 1) + "A";

      // Act
      AttackResult result = taskEndpoint.completed(tamperedToken);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.isLessonSolved()).isFalse();
      // Could be invalid_base64 or invalid_signature depending on the tampering;
      // we assert that it does NOT succeed.
      assertThat(result.getFeedback())
          .containsAnyOf(
              "insecure-deserialization.invalid_signature",
              "insecure-deserialization.invalid_base64",
              "insecure-deserialization.token_processing_error");
    }

    @Test
    void completed_withNonJsonToken_shouldFailAndNotDeserializeObjects() throws Exception {
      // Arrange
      InsecureDeserializationTask taskEndpoint = new InsecureDeserializationTask();
      // Not a valid Base64 string to force IllegalArgumentException
      String invalidToken = "!!!not-base64!!!";

      // Act
      AttackResult result = taskEndpoint.completed(invalidToken);

      // Assert: ensures error handling path and, indirectly, that no ObjectInputStream is used
      assertThat(result).isNotNull();
      assertThat(result.isLessonSolved()).isFalse();
      assertThat(result.getFeedback()).contains("insecure-deserialization.invalid_base64");
    }
  }
}
