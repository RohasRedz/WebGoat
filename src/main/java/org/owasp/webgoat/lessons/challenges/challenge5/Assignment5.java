package org.owasp.webgoat.lessons.challenges.challenge5;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.owasp.webgoat.container.assignments.AssignmentTask;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
public class Assignment5 extends AssignmentTask {

    @Autowired
    private DataSource dataSource;

    @PostMapping("/challenge5/search")
    @ResponseBody
    public AttackResult searchUser(@RequestParam String username) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return success().feedback("challenge5.success").build();
                    } else {
                        return failed().feedback("challenge5.failed").build();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during user search: " + e.getMessage());
            return failed().feedback("challenge5.error").build();
        }
    }
}
