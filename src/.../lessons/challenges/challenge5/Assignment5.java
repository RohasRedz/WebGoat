package org.owasp.webgoat.lessons.challenges.challenge5;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// import java.sql.Statement; // Removed

@Controller
public class Assignment5 {

    @PostMapping("/challenge/5/assignment")
    @ResponseBody
    public String submitAssignment(@RequestParam String name) {
        // Fixed: Using PreparedStatement to prevent SQL Injection
        String query = "SELECT * FROM users WHERE name = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return "User found: " + rs.getString("name");
                } else {
                    return "User not found.";
                }
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
}
