package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;

class SqlInjectionLesson6bTest {

  @Test
  @DisplayName("getPassword should not print stack traces to System.err when exceptions occur")
  void getPasswordDoesNotLeakStackTrace() {
    LessonDataSource dataSource = org.mockito.Mockito.mock(LessonDataSource.class);
    SqlInjectionLesson6b lesson = new SqlInjectionLesson6b(dataSource);

    PrintStream originalErr = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));

    try {
      String password = lesson.getPassword();

      String capturedErr = errContent.toString();
      org.junit.jupiter.api.Assertions.assertTrue(
          capturedErr.isEmpty(),
          "getPassword should not print stack traces or error details to System.err");
      assertEquals("dave", password);
    } finally {
      System.setErr(originalErr);
    }
  }
}
