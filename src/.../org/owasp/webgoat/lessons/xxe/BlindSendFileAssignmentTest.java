// Delta_UnitTest_Agent
// Package inferred from source file path; adjust if actual package differs.
// TODO: Confirm this package matches the actual BlindSendFileAssignment.java package.
package org.owasp.webgoat.lessons.xxe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Delta tests for BlindSendFileAssignment focusing on path traversal protection.
 *
 * Original (vulnerable) behavior (conceptual):
 *   - Constructed Paths directly from user-controlled filename, allowing traversal.
 *
 * Updated (secure) behavior:
 *   - Uses a fixed BASE_DIRECTORY.
 *   - Resolves filename against BASE_DIRECTORY and normalizes.
 *   - Ensures requestedPath.startsWith(BASE_DIRECTORY).
 *   - Rejects filenames containing ".." or starting with "/" or "\\".
 *   - Ensures file exists and is a regular file.
 */
class BlindSendFileAssignmentTest {

    private final BlindSendFileAssignment assignment = new BlindSendFileAssignment();

    // NOTE: We assume BASE_DIRECTORY is /tmp/webgoat_files/ as in the updated code.
    private static final Path BASE_DIRECTORY =
            Paths.get("/tmp/webgoat_files/").toAbsolutePath().normalize();

    @AfterEach
    void cleanup() throws IOException {
        // Clean up any files created under BASE_DIRECTORY during tests
        if (Files.exists(BASE_DIRECTORY) && Files.isDirectory(BASE_DIRECTORY)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(BASE_DIRECTORY)) {
                for (Path path : stream) {
                    Files.deleteIfExists(path);
                }
            } catch (NoSuchFileException ignored) {
                // ignore
            }
        }
    }

    @Test
    @DisplayName("Should read file only from within the BASE_DIRECTORY")
    void shouldReadFileWithinBaseDirectory() throws Exception {
        // Arrange
        Files.createDirectories(BASE_DIRECTORY);
        Path safeFile = BASE_DIRECTORY.resolve("safe.txt").normalize();
        Files.writeString(safeFile, "safe-content");

        // Act
        String content = assignment.readFileContent("safe.txt");

        // Assert
        assertThat(content).isEqualTo("safe-content");
    }

    @Test
    @DisplayName("Should reject path traversal attempts escaping BASE_DIRECTORY")
    void shouldRejectPathTraversalAttempt() {
        // Arrange
        String traversalFilename = "../etc/passwd";

        // Act & Assert
        // The updated code is expected to throw a SecurityException when traversal is detected.
        assertThatThrownBy(() -> assignment.readFileContent(traversalFilename))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Attempted path traversal");
    }

    @Test
    @DisplayName("Should reject absolute path filenames")
    void shouldRejectAbsolutePaths() {
        // Arrange
        String absoluteUnix = "/etc/passwd";
        String absoluteWindows = "C:\\secret.txt";

        // Act & Assert
        assertThatThrownBy(() -> assignment.readFileContent(absoluteUnix))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid characters or absolute path in filename");

        assertThatThrownBy(() -> assignment.readFileContent(absoluteWindows))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid characters or absolute path in filename");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null or blank filenames")
    void shouldRejectNullOrBlankFilenames() {
        assertThatThrownBy(() -> assignment.readFileContent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename cannot be empty");

        assertThatThrownBy(() -> assignment.readFileContent("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Filename cannot be empty");
    }

    @Test
    @DisplayName("Should throw IOException when file does not exist or is not regular file")
    void shouldThrowIOExceptionWhenFileMissingOrNotRegular() {
        // Non-existent file
        assertThatThrownBy(() -> assignment.readFileContent("nonexistent.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File not found or is not a regular file");
    }

    @Test
    @DisplayName("Should handle invalid path characters via IOException wrapping InvalidPathException")
    void shouldWrapInvalidPathExceptionAsIOException() {
        // Arrange: Using characters likely to cause InvalidPathException on most platforms.
        String invalidFilename = "\0illegal";

        // Act & Assert
        assertThatThrownBy(() -> assignment.readFileContent(invalidFilename))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid file path provided");
    }
}
