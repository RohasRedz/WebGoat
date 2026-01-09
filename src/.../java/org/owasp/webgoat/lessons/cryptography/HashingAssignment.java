package org.owasp.webgoat.lessons.cryptography;

import java.security.SecureRandom; // Added import

public class HashingAssignment {

    public String generateWeakToken() {
        SecureRandom secureRandom = new SecureRandom(); // Replaced Random with SecureRandom
        return String.valueOf(secureRandom.nextInt());
    }
}
