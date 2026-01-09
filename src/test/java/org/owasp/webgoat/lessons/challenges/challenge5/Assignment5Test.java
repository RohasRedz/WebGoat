package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Delta unit tests for Assignment5 focusing on the SQL injection fix.
 *
 * This test class is intended to reside at:
 * src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java
 * derived from the source path:
 * src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java
 */
public class Assignment5Test {

    @Test
    @DisplayName("login should not be vulnerable to SQL injection and only succeed for valid credentials")
    void login_shouldResistSqlInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSetValid = Mockito.mock(ResultSet.class);
        ResultSet resultSetInjection = Mockito.mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);

        // For a valid login attempt we expect one row
        when(preparedStatement.executeQuery())
                .thenReturn(resultSetValid)   // first call (valid credentials)
                .thenReturn(resultSetInjection); // second call (injection payload)

        when(resultSetValid.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        // For an injection attempt with wrong password, even if crafted to try injection,
        // the parameterized query should not allow bypass; result set should be empty.
        when(resultSetInjection.next()).thenReturn(false);

        // Act
        AttackResult validResult =
                assignment5.login("Larry", "correct-password");

        AttackResult injectionResult =
                assignment5.login("Larry", "wrong-password' OR '1'='1");

        // Assert
        // Valid credentials should succeed
        assertEquals(AttackResult.Status.SUCCESS, validResult.getLessonStatus(),
                "Expected successful login for correct credentials");

        // Injection attempt with incorrect password must fail, proving the query
        // is parameterized and no longer interprets the payload as SQL
        assertEquals(AttackResult.Status.FAIL, injectionResult.getLessonStatus(),
                "SQL injection payload must not bypass authentication");
    }

    @Test
    @DisplayName("login should fail when username is not Larry, even with SQL injection payload")
    void login_shouldFailForNonLarryEvenWithInjection() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult result =
                assignment5.login("Mallory' OR '1'='1", "anything");

        // Assert
        // This exercises the pre-condition check before any DB call and confirms
        // that injection in the username is not used to bypass the "Larry" check.
        assertEquals(AttackResult.Status.FAIL, result.getLessonStatus(),
                "Non-Larry user with injection payload must not bypass username check");
    }
}
