package org.owasp.webgoat.lessons.xxe;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delta tests for BlindSendFileAssignment focusing on the vulnerability fix:
 * preventing path traversal and enforcing a safe base directory.
 */
class BlindSendFileAssignmentTest {

    @Test
    void getFileWithSimpleFilenameResolvesWithinBaseDirectory() {
        BlindSendFileAssignment assignment = new BlindSendFileAssignment();

        File file = assignment.getFile("profile.png");

        assertNotNull(file, "File should be returned for a simple, safe filename");
        String path = file.getAbsolutePath();
        assertTrue(path.contains("/tmp/webgoat_files/"),
                "Resolved path must reside under the configured BASE_DIR");
        assertFalse(path.contains(".."),
                "Normalized path must not contain any path traversal segments");
    }

    @Test
    void getFileWithPathTraversalIsRejected() {
        BlindSendFileAssignment assignment = new BlindSendFileAssignment();

        File file = assignment.getFile("../etc/passwd");

        assertNull(file, "Path traversal attempts must be rejected and return null");
    }

    @Test
    void getFileWithAbsolutePathIsRejected() {
        BlindSendFileAssignment assignment = new BlindSendFileAssignment();

        // On Unix-like systems, this is an absolute path. On other systems,
        // the logic still checks for absolute or drive-letter paths.
        File file = assignment.getFile("/etc/passwd");

        assertNull(file, "Absolute paths must be rejected by validation logic");
    }
}
