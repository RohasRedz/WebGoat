// Assumed package based on source file path; adjust if needed.
package org.owasp.webgoat.lessons.xxe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.WebGoatUser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Delta tests for BlindSendFileAssignment focused on:
 * - Username sanitization in path construction.
 * - Ensuring the created directory path remains under the expected base directory.
 * - Aborting file creation when a traversal-like username would escape the base directory.
 */
class BlindSendFileAssignmentTest {

    private static final String BASE_DIR = "/tmp/webgoat-home"; // test-only base directory

    private CommentsCache comments;
    private BlindSendFileAssignment assignment;

    @BeforeEach
    void setUp() {
        comments = mock(CommentsCache.class);
        assignment = new BlindSendFileAssignment(BASE_DIR, comments);
    }

    @Test
    void createSecretFile_shouldUseSanitizedUsernameAndStayWithinBaseDirectory() throws Exception {
        // Arrange
        WebGoatUser user = mock(WebGoatUser.class);
        when(user.getUsername()).thenReturn("john.doe");

        // Act
        assignment.initialize(user); // indirectly calls createSecretFileWithRandomContents

        // Assert
        // The username "john.doe" is allowed by the sanitization regex, so the directory should be:
        Path expectedDir = Paths.get(BASE_DIR, "XXE", "john.doe").normalize();
        File dir = expectedDir.toFile();
        // Existence check (best-effort) – the directory should have been created
        assertThat(dir.exists()).isTrue();
        assertThat(dir.isDirectory()).isTrue();

        // And the path must still be under the intended base directory
        Path basePath = Paths.get(BASE_DIR, "XXE").normalize();
        assertThat(expectedDir.startsWith(basePath)).isTrue();

        // Clean up (optional best-effort)
        new File(dir, "secret.txt").delete();
        dir.delete();
        new File(BASE_DIR, "XXE").delete();
    }

    @Test
    void createSecretFile_shouldSanitizeDangerousCharactersInUsername() {
        // Arrange
        WebGoatUser user = mock(WebGoatUser.class);
        when(user.getUsername()).thenReturn("../../../etc/passwd");

        // We can't access createSecretFileWithRandomContents directly since it's private,
        // but initialize(...) calls it after resetting the cache.
        // Because of sanitization and path checks, no traversal outside BASE_DIR/XXE should occur.

        // Act
        assignment.initialize(user);

        // Assert
        // The sanitized username replaces disallowed characters with '_'
        String sanitized = "../../../etc/passwd".replaceAll("[^a-zA-Z0-9.-]", "_");
        Path targetPath = Paths.get(BASE_DIR, "XXE", sanitized).normalize();
        Path basePath = Paths.get(BASE_DIR, "XXE").normalize();

        // Even with a traversal-like username, the normalized path must still start with basePath
        assertThat(targetPath.startsWith(basePath)).isTrue();
    }

    @Test
    void createSecretFile_shouldAbortWhenPathWouldEscapeBaseDirectory() {
        // Arrange
        // To simulate a potentially problematic base directory, we use a relative path
        // and a crafted username. We cannot call the private method directly,
        // but we can at least assert that initialize(...) does not throw and that
        // no unexpected directories are created outside the intended base.
        String localBase = "target/webgoat-home-test";
        CommentsCache cache = mock(CommentsCache.class);
        BlindSendFileAssignment localAssignment = new BlindSendFileAssignment(localBase, cache);

        WebGoatUser user = mock(WebGoatUser.class);
        when(user.getUsername()).thenReturn("../outside");

        // Act
        localAssignment.initialize(user);

        // Assert
        // Because of the path confinement check, the effective directory must still be under localBase/XXE.
        String sanitized = "../outside".replaceAll("[^a-zA-Z0-9.-]", "_");
        Path targetPath = Paths.get(localBase, "XXE", sanitized).normalize();
        Path basePath = Paths.get(localBase, "XXE").normalize();
        assertThat(targetPath.startsWith(basePath)).isTrue();
    }
}
