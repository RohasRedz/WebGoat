// TODO: Adjust the package to match the actual source package if different.
package org.owasp.webgoat.lessons.sqlinjection.advanced;

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

class SqlInjectionChallengeTest {

    @Test
    @DisplayName("executeQuery should use PreparedStatement and not concatenate user input into SQL")
    void executeQuery_usesPreparedStatement() throws Exception {
        // Arrange
        SqlInjectionChallenge controller = new SqlInjectionChallenge();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String input = "widget";
        String expectedSql = "SELECT * FROM products WHERE name = ?";

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        try (MockedStatic<DriverManager> dm = Mockito.mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(mockConnection);

            when(mockConnection.prepareStatement(expectedSql)).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, true, false);
            when(mockResultSet.getString("name")).thenReturn("widget1", "widget2");
            when(mockResultSet.getString("description")).thenReturn("desc1", "desc2");

            // Act & Assert
            mockMvc.perform(post("/sqlinjection/challenge")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("queryParam", input))
                   .andExpect(status().isOk())
                   .andExpect(content().string("widget1: desc1<br/>widget2: desc2<br/>"));

            // Assert secure behavior: SQL is parameterized
            verify(mockConnection, times(1)).prepareStatement(expectedSql);
            verify(mockPreparedStatement, times(1)).setString(1, input);
            verify(mockPreparedStatement, times(1)).executeQuery();
        }
    }

    @Test
    @DisplayName("executeQuery should handle no results without breaking the secure query pattern")
    void executeQuery_handlesNoResults() throws Exception {
        // Arrange
        SqlInjectionChallenge controller = new SqlInjectionChallenge();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String input = "nonexistent";

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        try (MockedStatic<DriverManager> dm = Mockito.mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
              .thenReturn(mockConnection);

            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/sqlinjection/challenge")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("queryParam", input))
                   .andExpect(status().isOk())
                   .andExpect(content().string(""));

            // Still must use PreparedStatement even if no rows are returned
            verify(mockPreparedStatement, times(1)).setString(1, input);
        }
    }
}
