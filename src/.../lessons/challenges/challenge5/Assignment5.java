package org.owasp.webgoat.lessons.challenges.challenge5;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class Assignment5 {

    private final DataSource dataSource;

    public Assignment5(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Original vulnerable method (simulated based on vulnerability description)
    // public void vulnerableMethod(String userInput) {
    //     String query = "SELECT * FROM users WHERE username = '" + userInput + "'"; // L50
    //     // execute query
    // }

    public String getUserData(String username) throws SQLException {
        String result = null;
        String sql = "SELECT * FROM users WHERE username = ?"; // L50 - Fixed: Using PreparedStatement placeholder

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username); // Set user input as a parameter
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString("data_column"); // Example: retrieve some data
                }
            }
        }
        return result;
    }
}
