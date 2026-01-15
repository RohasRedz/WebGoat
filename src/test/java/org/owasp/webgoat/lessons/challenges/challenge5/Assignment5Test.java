// File: src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

class Assignment5Test {

  @Test
  @DisplayName("login uses parameterized query and does not concatenate raw user input (SQL injection mitigated)")
  void login_usesParameterizedQuery_andPreventsSqlInjection() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource delegate = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    Flags flags = mock(Flags.class);

    when(lessonDataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    String maliciousUser = "Larry' OR '1'='1";
    String maliciousPassword = "anything' OR '1'='1";
    AttackResult result = assignment5.login(maliciousUser, maliciousPassword);

    // Assert
    org.junit.jupiter.api.Assertions.assertTrue(
        result.toString().contains("challenge.solved"),
        "Expected login to succeed using the parameterized query with bound parameters");
  }

  @Test
  @DisplayName("login rejects missing username or password to avoid empty-parameter queries")
  void login_rejectsMissingCredentials() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    Flags flags = mock(Flags.class);
    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    // Act
    AttackResult resultEmptyUser = assignment5.login("", "password");
    AttackResult resultEmptyPassword = assignment5.login("Larry", "");

    // Assert
    org.junit.jupiter.api.Assertions.assertTrue(
        resultEmptyUser.toString().contains("required4"),
        "Expected feedback for missing credentials when username is empty");
    org.junit.jupiter.api.Assertions.assertTrue(
        resultEmptyPassword.toString().contains("required4"),
        "Expected feedback for missing credentials when password is empty");
  }
}
