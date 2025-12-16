// TODO: Adjust the package to match the actual source package if different.
package org.owasp.webgoat.lessons.challenges.challenge5;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Assignment5Test {

    @Test
    @DisplayName("submitAssignment should use PreparedStatement parameter binding and not concatenate input into SQL")
    void submitAssignment_usesPreparedStatementAndReturnsUserFound() throws Exception {
        // Arrange
        Assignment5 controller = new Assignment5();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String inputName = "alice";
        String expectedSql = "SELECT * FROM users WHERE name = ?";

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        try (MockedStatic<DriverManager> dm = Mockito.mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(mockConnection);

            when(mockConnection.prepareStatement(expectedSql)).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("name")).thenReturn(inputName);

            // Act & Assert
            mockMvc.perform(post("/challenge/5/assignment")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("name", inputName))
                   .andExpect(status().isOk())
                   .andExpect(content().string("User found: " + inputName));

            // Assert secure behavior: input bound as parameter, not concatenated into SQL
            verify(mockConnection, times(1)).prepareStatement(expectedSql);
            verify(mockPreparedStatement, times(1)).setString(1, inputName);
            verify(mockPreparedStatement, times(1)).executeQuery();
        }
    }

    @Test
    @DisplayName("submitAssignment should return 'User not found.' when no rows are returned")
    void submitAssignment_returnsUserNotFoundWhenNoResult() throws Exception {
        // Arrange
        Assignment5 controller = new Assignment5();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String inputName = "unknown";

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        try (MockedStatic<DriverManager> dm = Mockito.mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(mockConnection);

            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act & Assert (functional behavior untouched by the security fix)
            mockMvc.perform(post("/challenge/5/assignment")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("name", inputName))
                   .andExpect(status().isOk())
                   .andExpect(content().string("User not found."));
        }
    }
}
