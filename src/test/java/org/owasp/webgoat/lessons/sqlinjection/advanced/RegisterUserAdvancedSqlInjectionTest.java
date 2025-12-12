package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AttackResult;

public class RegisterUserAdvancedSqlInjectionTest {

    private final LessonDataSource dataSource = mock(LessonDataSource.class);
    private final RegisterUserAdvancedSqlInjection lesson =
            new RegisterUserAdvancedSqlInjection(dataSource);

    @Test
    void registerNewUser_shouldUsePreparedStatementAndTreatUsernameAsLiteral_whenUserDoesNotExist()
            throws Exception {
        String username = "john'; DROP TABLE sql_challenge_users; --";
        String email = "john@example.com";
        String password = "password123";

        Connection connection = mock(Connection.class);
        PreparedStatement selectPs = mock(PreparedStatement.class);
        ResultSet selectRs = mock(ResultSet.class);
        PreparedStatement insertPs = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
                .thenReturn(selectPs);
        when(selectPs.executeQuery()).thenReturn(selectRs);
        when(selectRs.next()).thenReturn(false);

        when(connection.prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)")).thenReturn(insertPs);

        AttackResult result = lesson.registerNewUser(username, email, password);

        assertNotNull(result);

        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(selectPs).setString(eq(1), usernameCaptor.capture());
        assertEquals(username, usernameCaptor.getValue());

        verify(selectPs).executeQuery();
        verify(insertPs).setString(1, username);
        verify(insertPs).setString(2, email);
        verify(insertPs).setString(3, password);
        verify(insertPs).execute();
    }

    @Test
    void registerNewUser_shouldReturnUserExists_whenSelectFindsExistingUser() throws Exception {
        String username = "existingUser";
        String email = "user@example.com";
        String password = "password123";

        Connection connection = mock(Connection.class);
        PreparedStatement selectPs = mock(PreparedStatement.class);
        ResultSet selectRs = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select userid from sql_challenge_users where userid = ?"))
                .thenReturn(selectPs);
        when(selectPs.executeQuery()).thenReturn(selectRs);
        when(selectRs.next()).thenReturn(true);

        AttackResult result = lesson.registerNewUser(username, email, password);

        assertNotNull(result);

        verify(selectPs).setString(1, username);
        verify(selectPs).executeQuery();
        verify(connection, never()).prepareStatement("INSERT INTO sql_challenge_users VALUES (?, ?, ?)");
    }
}
