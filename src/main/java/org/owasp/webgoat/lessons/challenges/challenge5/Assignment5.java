/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.challenges.challenge5;

import org.owasp.webgoat.lessons.AbstractLesson;
import org.owasp.webgoat.lessons.LessonData;
import org.owasp.webgoat.lessons.LessonDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Lesson demonstrating secure query execution using PreparedStatement to prevent SQL injection.
 */
@Component
public class Assignment5 extends AbstractLesson {

    @Autowired
    private LessonDataRepository lessonDataRepository;

    @Override
    public void start(@NotNull @Size(min = 1, max = 50) LessonData lessonData) {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:webgoat", "sa", "")) {
            String userInput = lessonData.getUserInput();

            // ✅ Validate user input before using it in queries
            if (userInput == null || userInput.trim().isEmpty()) {
                throw new IllegalArgumentException("Username must not be empty");
            }

            // ✅ Use parameterized PreparedStatement to prevent SQL injection
            String query = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, userInput.trim());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("User: " + rs.getString("username"));
                    }
                }
            }
        } catch (Exception e) {
            // Avoid printing sensitive details in production
            e.printStackTrace();
        }
    }
}