package org.owasp.webgoat.lessons.xxe;

import java.io.File;
import java.nio.file.InvalidPathException; // Added import
import java.nio.file.Path;
import java.nio.file.Paths;

public class BlindSendFileAssignment {

    private static final String BASE_DIR = "/tmp/webgoat_files/"; // Hypothetical base directory

    public File getFile(String filename) {
        try {
            // Normalize the path to resolve ".." and "." components
            Path requestedPath = Paths.get(filename).normalize();

            // Construct the absolute path by resolving against the base directory
            Path resolvedPath = Paths.get(BASE_DIR).resolve(requestedPath);

            // Validate that the resolved path is still within the base directory
            // This prevents path traversal attacks
            if (!resolvedPath.startsWith(Paths.get(BASE_DIR))) {
                // Log suspicious activity and throw an exception or return null
                System.err.println("Path traversal attempt detected: " + filename);
                return null; // Or throw new SecurityException("Invalid file path");
            }

            // Further validation: ensure the filename itself doesn't contain malicious characters
            // For example, disallow absolute paths or Windows drive letters if not expected
            if (requestedPath.isAbsolute() || requestedPath.toString().contains(":") || requestedPath.toString().contains("\\")) {
                 System.err.println("Invalid filename format: " + filename);
                 return null;
            }

            return resolvedPath.toFile();
        } catch (InvalidPathException e) {
            System.err.println("Invalid path format provided: " + filename + " - " + e.getMessage());
            return null;
        }
    }
}
