package org.owasp.webgoat.lessons.xxe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException; // Added import
import java.nio.file.Path;
import java.nio.file.Paths;

public class BlindSendFileAssignment {

    // Define a secure base directory where files are allowed to be read from.
    // This should be configured securely and ideally not be user-writable.
    private static final Path BASE_DIRECTORY = Paths.get("/tmp/webgoat_files/").toAbsolutePath().normalize(); // L57 - Fixed: Define base directory securely

    public String readFileContent(String filename) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty.");
        }

        try {
            // Construct the path by resolving the filename against the base directory.
            // This prevents absolute paths from escaping the base directory.
            Path requestedPath = BASE_DIRECTORY.resolve(filename).normalize(); // L57 - Fixed: Resolve and normalize

            // Critical security check: Ensure the normalized path is still within the base directory.
            // This prevents path traversal attacks like "../../../etc/passwd".
            if (!requestedPath.startsWith(BASE_DIRECTORY)) {
                throw new SecurityException("Attempted path traversal: " + filename);
            }

            // Further checks: Reject if the filename itself tries to use '..' or is an absolute path
            // (though resolve().normalize() should handle most of this, explicit checks add defense-in-depth).
            if (filename.contains("..") || filename.startsWith("/") || filename.startsWith("\\")) {
                throw new SecurityException("Invalid characters or absolute path in filename: " + filename);
            }

            // Ensure the file exists and is a regular file before reading
            if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
                throw new IOException("File not found or is not a regular file: " + filename);
            }

            return Files.readString(requestedPath);
        } catch (InvalidPathException e) {
            throw new IOException("Invalid file path provided: " + filename, e);
        }
    }
}
