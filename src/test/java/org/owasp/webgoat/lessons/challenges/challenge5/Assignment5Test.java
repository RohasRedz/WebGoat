// Delta_UnitTest_Agent
// NOTE: This test focuses specifically on the SQL injection–related behavior introduced by the fix.
// Test path inferred from main path by replacing 'main' with 'test':
// src/test/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5Test.java

package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

class Assignment5Test {

    @Test
    @DisplayName("login uses a parameterized query and does not concatenate user input into SQL")
    void login_usesParameterizedQuery_andBindsUserInput() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        String username = "Larry";
        String password = "pa$$word' OR '1'='1"; // classic injection payload

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sqlCaptor.capture());

        String usedSql = sqlCaptor.getValue();
        // Ensure that the raw user input is NOT directly concatenated into the SQL string.
        assertThat(usedSql)
                .doesNotContain(username)
                .doesNotContain(password)
                .contains("where userid = ?")
                .contains("and password = ?");

        // Ensure values are bound as parameters in correct order
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);

        // And the overall functional behavior remains success for valid credentials
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    @DisplayName("login rejects empty username or password before reaching SQL layer")
    void login_rejectsEmptyInput_beforeQueryExecution() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        AttackResult resultEmptyUser = assignment5.login("", "pwd");
        AttackResult resultEmptyPwd = assignment5.login("Larry", " ");

        // Assert
        assertThat(resultEmptyUser.getLessonCompleted()).isFalse();
        assertThat(resultEmptyPwd.getLessonCompleted()).isFalse();

        // Ensure no DB interaction when input is invalid
        verifyNoInteractions(dataSource);
    }
}
