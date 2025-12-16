// Delta unit test for Assignment5.java
// Assumed package based on resolved_file_path; adjust if actual package differs.
package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.challenges.Flags;

class Assignment5Test {

    @Test
    void loginShouldUseParameterizedQueryAndAuthenticateSuccessfully() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(flags.getFlag(5)).thenReturn("FLAG-5");

        String username = "Larry";
        String password = "p@ss";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isTrue();
    }

    @Test
    void loginShouldFailWhenCredentialsDoNotMatch() throws Exception {
        // Arrange
        LessonDataSource dataSource = mock(LessonDataSource.class);
        Flags flags = mock(Flags.class);
        Assignment5 assignment5 = new Assignment5(dataSource, flags);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "select password from challenge_users where userid = ? and password = ?"))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String username = "Larry";
        String password = "wrong";

        // Act
        AttackResult result = assignment5.login(username, password);

        // Assert
        verify(connection).prepareStatement(
                "select password from challenge_users where userid = ? and password = ?");
        verify(preparedStatement).setString(1, username);
        verify(preparedStatement).setString(2, password);
        verify(preparedStatement).executeQuery();

        assertThat(result).isNotNull();
        assertThat(result.getLessonCompleted()).isFalse();
    }
}
