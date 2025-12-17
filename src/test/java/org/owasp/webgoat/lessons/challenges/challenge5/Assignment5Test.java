// Assumed package based on main file path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.lessons.challenges.Flags;
import org.springframework.util.StringUtils;

/**
 * Delta tests for Assignment5 focusing on:
 * - SQL injection fix (use of parameterized query instead of string concatenation).
 */
class Assignment5Test {

    @Test
    void login_shouldUseParameterizedQueryWithUserInputs() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry' OR '1'='1";
        String password = "pass' OR '1'='1";

        // Act
        assignment5.login(username, password);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(connection).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();

        // verify SQL shape uses parameter markers instead of direct concatenation
        assertThat(sql)
                .isEqualTo("select password from challenge_users where userid = ? and password = ?");

        // verify parameters are bound via setString (no concatenation)
        Mockito.verify(preparedStatement).setString(1, username);
        Mockito.verify(preparedStatement).setString(2, password);
    }

    @Test
    void login_shouldRejectEmptyInputAsBefore() throws Exception {
        // Arrange
        LessonDataSource dataSource = Mockito.mock(LessonDataSource.class);
        Flags flags = Mockito.mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        // Act
        var result = assignment5.login("", "   ");

        // Assert
        assertThat(StringUtils.hasText("")).isFalse(); // sanity check for behavior
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
