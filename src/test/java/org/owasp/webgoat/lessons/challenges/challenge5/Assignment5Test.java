package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Delta tests for Assignment5 focusing on the parameterized SQL change.
 *
 * File under test:
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
 */
class Assignment5Test {

  @Test
  @DisplayName("login() should use PreparedStatement with parameters instead of string concatenation")
  void loginUsesPreparedStatementParameters() throws Exception {
    // Arrange
    LessonDataSource lessonDataSource = mock(LessonDataSource.class);
    DataSource delegateDataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Flags flags = mock(Flags.class);

    // The updated Assignment5 uses LessonDataSource#getConnection() which delegates to a DataSource
    // Mock the internal DataSource and Connection/PreparedStatement chain
    org.mockito.Mockito.when(lessonDataSource.getConnection()).thenReturn(connection);
    org.mockito.Mockito.when(connection.prepareStatement(
            "select password from challenge_users where userid = ? and password = ?"))
        .thenReturn(preparedStatement);

    // Mock request context used by AttackResultBuilder in the WebGoat framework
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    Assignment5 assignment5 = new Assignment5(lessonDataSource, flags);

    String username = "Larry";
    String password = "safePassword123";

    // Act
    assignment5.login(username, password);

    // Assert
    // Critical delta assertions: parameter binding must be used on the PreparedStatement
    verify(connection)
        .prepareStatement(
            eq("select password from challenge_users where userid = ? and password = ?"));
    verify(preparedStatement).setString(1, username);
    verify(preparedStatement).setString(2, password);
  }
}
