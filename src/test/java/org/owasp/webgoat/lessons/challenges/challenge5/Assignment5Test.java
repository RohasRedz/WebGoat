/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Delta tests for Assignment5 focusing on the SQL injection fix.
 *
 * Changed behavior:
 * - Before: SQL query was built via string concatenation with user input.
 * - After: SQL query uses parameter placeholders and binds user input via setString().
 *
 * These tests:
 * - Verify that the fixed code still authenticates correctly when valid credentials are supplied.
 * - Verify that an attempted SQL injection payload is treated as a normal credential and does not succeed.
 */
public class Assignment5Test {

  private LessonDataSource lessonDataSource;
  private Flags flags;
  private Assignment5 assignment5;

  @BeforeEach
  void setUp() throws Exception {
    // Configure in-memory H2 database
    JdbcConnectionPool pool =
        JdbcConnectionPool.create(
            "jdbc:h2:mem:test-assignment5;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    Connection connection = pool.getConnection();

    // Create schema and test data expected by Assignment5
    connection
        .createStatement()
        .execute(
            "CREATE TABLE challenge_users (userid VARCHAR(100) PRIMARY KEY, password VARCHAR(255))");
    // The lesson expects user 'Larry' to exist
    connection
        .createStatement()
        .execute("INSERT INTO challenge_users(userid, password) VALUES ('Larry', 'secret-pass')");

    DataSource ds = new SingleConnectionDataSource(connection, true);
    lessonDataSource = new LessonDataSource(ds);

    flags = org.mockito.Mockito.mock(Flags.class);
    when(flags.getFlag(5)).thenReturn("FLAG-5");

    assignment5 = new Assignment5(lessonDataSource, flags);
  }

  @Test
  void login_withValidCredentials_succeedsAndReturnsSuccessResult() throws Exception {
    // Arrange
    String username = "Larry";
    String password = "secret-pass";

    // Act
    AttackResult result = assignment5.login(username, password);

    // Assert
    // The attack should be considered successful for the correct user & password
    assertEquals(AttackResult.Status.SUCCESS, result.getStatus());
    assertEquals("FLAG-5", result.getFeedbackArgs()[0]);
    // Ensure flag access still occurs (behavior preserved)
    verify(flags).getFlag(5);
  }

  @Test
  void login_withSqlInjectionPayloadInPassword_doesNotBypassAuthentication() throws Exception {
    // Arrange
    String username = "Larry";
    // Attempt to exploit SQL injection; with parameter binding this must not work
    String maliciousPassword = "wrong' OR '1'='1";

    // Act
    AttackResult result = assignment5.login(username, maliciousPassword);

    // Assert
    // With parameterized query, the injection payload is treated as a literal password
    // and must not authenticate successfully.
    assertEquals(AttackResult.Status.FAIL, result.getStatus());
  }
}
