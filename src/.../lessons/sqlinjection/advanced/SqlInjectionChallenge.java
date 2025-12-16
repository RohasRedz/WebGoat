package org.owasp.webgoat.lessons.sqlinjection.advanced;

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
public class SqlInjectionChallenge {

    @PostMapping("/sqlinjection/challenge")
    @ResponseBody
    public String executeQuery(@RequestParam String queryParam) {
        // Fixed: Using PreparedStatement to prevent SQL Injection
        String sql = "SELECT * FROM products WHERE name = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, queryParam);
            try (ResultSet rs = pstmt.executeQuery()) {
                StringBuilder result = new StringBuilder();
                while (rs.next()) {
                    result.append(rs.getString("name")).append(": ").append(rs.getString("description")).append("<br/>");
                }
                return result.toString();
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
}
