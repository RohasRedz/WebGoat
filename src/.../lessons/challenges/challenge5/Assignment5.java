package org.owasp.webgoat.lessons.challenges.challenge5;

import java.sql.Connection;
import java.sql.PreparedStatement; // Added import
import java.sql.ResultSet;
import java.sql.SQLException;
// import java.sql.Statement; // Removed import

public class Assignment5 {

    public String getUserData(Connection connection, String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?"; // Parameterized query
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username); // Setting parameter
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("data");
                }
            }
        }
        return null;
    }
}
