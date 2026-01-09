package org.owasp.webgoat.lessons.cryptography;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delta tests for HashingAssignment focusing on the vulnerability fix:
 * using SecureRandom instead of a weak PRNG.
 */
class HashingAssignmentTest {

    /**
     * Verifies that generateWeakToken uses a cryptographically strong RNG by:
     * - Producing varying values across multiple invocations (basic non-determinism check).
     * - Returning a numeric string (same visible contract as before).
     *
     * NOTE: We cannot assert the exact RNG type without reflection into private fields,
     * but this test ensures the contract remains and that values are not trivially constant.
     */
    @Test
    void generateWeakTokenProducesNonConstantNumericValues() {
        HashingAssignment assignment = new HashingAssignment();

        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String token = assignment.generateWeakToken();
            assertNotNull(token, "Token should not be null");
            assertFalse(token.isEmpty(), "Token should not be empty");
            assertDoesNotThrow(() -> Integer.parseInt(token), "Token should be parseable as integer");
            tokens.add(token);
        }

        // With SecureRandom, it's extremely unlikely to get all identical tokens.
        assertTrue(tokens.size() > 1, "Expected more than one distinct token value");
    }

    /**
     * Repeated test to further guard against accidental deterministic/constant behavior.
     * Not a statistical randomness test, only a sanity check that values change run-to-run.
     */
    @RepeatedTest(5)
    void generateWeakTokenIsLikelyDifferentBetweenCalls() {
        HashingAssignment assignment = new HashingAssignment();
        String first = assignment.generateWeakToken();
        String second = assignment.generateWeakToken();
        assertNotEquals(first, second, "Two subsequent tokens should likely differ");
    }
}
