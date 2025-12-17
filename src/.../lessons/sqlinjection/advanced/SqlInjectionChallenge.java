package org.owasp.webgoat.lessons.sqlinjection.advanced;

import java.sql.Connection;
import java.sql.PreparedStatement; // Added import
import java.sql.ResultSet;
import java.sql.SQLException;
// import java.sql.Statement; // Removed import
import javax.sql.DataSource;

public class SqlInjectionChallenge {

    private final DataSource dataSource;

    public SqlInjectionChallenge(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getProductDetails(String productId) throws SQLException {
        // Fixed: Using PreparedStatement to prevent SQL Injection
        String query = "SELECT name, description FROM products WHERE id = ?"; // L57 - Fixed
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) { // Changed to PreparedStatement
            statement.setString(1, productId); // Set parameter
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name") + " - " + rs.getString("description");
                }
            }
        }
        return null;
    }
}
