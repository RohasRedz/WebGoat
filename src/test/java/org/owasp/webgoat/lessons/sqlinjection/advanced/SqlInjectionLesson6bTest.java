package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class SqlInjectionLesson6bTest {

  private LessonDataSource lessonDataSource;

  @BeforeEach
  void setUp() throws Exception {
    JdbcConnectionPool pool =
        JdbcConnectionPool.create(
            "jdbc:h2:mem:test-sqlinj6b;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    var connection = pool.getConnection();
    DataSource ds = new SingleConnectionDataSource(connection, true);
    lessonDataSource = new LessonDataSource(ds);
  }

  @Test
  void getPassword_whenSqlExceptionOccurs_returnsFallbackPasswordWithoutThrowing() {
    var lesson = new SqlInjectionLesson6b(lessonDataSource);
    String password = lesson.getPassword();
    assertEquals("dave", password);
  }

  @Test
  void completed_withWrongPasswordAfterException_returnsFailedAttackResult() throws Exception {
    var lesson = new SqlInjectionLesson6b(lessonDataSource);
    var result = lesson.completed("not-the-password");
    assertEquals(org.owasp.webgoat.container.assignments.AttackResult.Status.FAIL, result.getStatus());
  }
}
