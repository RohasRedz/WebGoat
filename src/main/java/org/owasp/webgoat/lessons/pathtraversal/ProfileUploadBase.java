/*
 * SPDX-FileCopyrightText: Copyright © 2020 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.pathtraversal;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.informationMessage;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.owasp.webgoat.container.CurrentUsername;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class ProfileUploadBase implements AssignmentEndpoint {

  private static final String UPLOAD_SUBDIRECTORY = "PathTraversal";
  // Pattern to allow only alphanumeric, dot, hyphen, underscore characters for filenames/usernames
  // This helps prevent path traversal characters like '/' or '..'
  private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[^a-zA-Z0-9.\\-_]");


  private final String webGoatHomeDirectory;

  public ProfileUploadBase(String webGoatHomeDirectory) {
    this.webGoatHomeDirectory = webGoatHomeDirectory;
  }

  protected AttackResult execute(MultipartFile file, String fullName, String username) {
    if (file.isEmpty()) {
      return failed(this).feedback("path-traversal-profile-empty-file").build();
    }
    if (StringUtils.isEmpty(fullName)) {
      return failed(this).feedback("path-traversal-profile-empty-name").build();
    }

    // Sanitize user-provided fullName and username
    String sanitizedFullName = sanitizeInput(fullName);
    String sanitizedUsername = sanitizeInput(username);

    File uploadDirectory;
    try {
      // Ensure the user's upload directory exists and is valid
      uploadDirectory = cleanupAndCreateDirectoryForUser(sanitizedUsername);
    } catch (IOException e) {
      return failed(this).output("Error creating upload directory: " + e.getMessage()).build();
    }

    try {
      // Get the canonical path of the upload directory for strict validation
      Path canonicalUploadDirectory = uploadDirectory.toPath().toRealPath();
      
      // Construct the target file path by resolving the sanitized filename against the canonical directory
      // Normalize to handle any '.' or '..' that might have slipped through (though sanitization should prevent most)
      Path targetFilePath = canonicalUploadDirectory.resolve(sanitizedFullName).normalize();

      // Crucial: Validate that the resolved target file path remains strictly within the canonical upload directory
      if (!targetFilePath.startsWith(canonicalUploadDirectory)) {
        // Path traversal attempt detected: the resolved path escapes the intended directory
        return failed(this).feedback("path-traversal-profile-attempt-detected").build();
      }

      File uploadedFile = targetFilePath.toFile();
      // Create the file only after validation
      uploadedFile.createNewFile();
      FileCopyUtils.copy(file.getBytes(), uploadedFile);

      if (attemptWasMade(uploadDirectory, uploadedFile)) {
        return solvedIt(uploadedFile);
      }
      return informationMessage(this)
          .feedback("path-traversal-profile-updated")
          .feedbackArgs(uploadedFile.getAbsoluteFile())
          .build();

    } catch (IOException e) {
      return failed(this).output(e.getMessage()).build();
    }
  }

  @SneakyThrows
  protected File cleanupAndCreateDirectoryForUser(String username) throws IOException {
    // Sanitize username again, though it should already be sanitized from execute method
    String sanitizedUsername = sanitizeInput(username);

    // Define the base path for all uploads
    Path baseUploadPath = Paths.get(this.webGoatHomeDirectory, UPLOAD_SUBDIRECTORY);
    // Ensure the base upload directory exists BEFORE resolving user-specific paths
    Files.createDirectories(baseUploadPath);

    // Resolve the user-specific upload directory path
    Path userUploadPath = baseUploadPath.resolve(sanitizedUsername);
    // Ensure the user-specific upload directory exists
    Files.createDirectories(userUploadPath);

    // Now that directories exist, canonicalize paths for robust validation
    Path canonicalBaseUploadPath = baseUploadPath.toRealPath();
    Path canonicalUserUploadPath = userUploadPath.toRealPath();

    // Ensure the resolved user upload path is still within the intended base directory
    if (!canonicalUserUploadPath.startsWith(canonicalBaseUploadPath)) {
      // This check should ideally not be hit if Files.createDirectories was successful and input sanitized
      // but serves as a final defense-in-depth.
      throw new IOException("Path traversal attempt detected during directory creation for user: " + username);
    }

    File uploadDirectory = canonicalUserUploadPath.toFile();
    // The original code had a deleteRecursively here, which might be lesson-specific.
    // If the intent is to always start fresh, keep it. Otherwise, remove.
    // For now, preserving original lesson behavior if it was intended to clear previous uploads.
    if (uploadDirectory.exists()) {
      FileSystemUtils.deleteRecursively(uploadDirectory);
      Files.createDirectories(uploadDirectory.toPath()); // Recreate after deletion
    }
    return uploadDirectory;
  }

  private boolean attemptWasMade(File expectedUploadDirectory, File uploadedFile)
      throws IOException {
    // Canonicalize paths for comparison to prevent traversal bypasses
    Path canonicalExpectedUploadDirectory = expectedUploadDirectory.toPath().toRealPath();
    Path canonicalUploadedFileParent = uploadedFile.getParentFile().toPath().toRealPath();

    return !canonicalExpectedUploadDirectory.equals(canonicalUploadedFileParent);
  }

  private AttackResult solvedIt(File uploadedFile) throws IOException {
    // Canonicalize path for comparison
    Path canonicalUploadedFilePath = uploadedFile.toPath().toRealPath();
    
    // Define the expected base directory for solving the lesson
    Path expectedLessonBasePath = Paths.get(this.webGoatHomeDirectory, UPLOAD_SUBDIRECTORY);
    // Ensure this path exists before calling toRealPath()
    Files.createDirectories(expectedLessonBasePath);
    Path canonicalExpectedLessonBasePath = expectedLessonBasePath.toRealPath();

    // Check if the uploaded file's parent directory is within the expected lesson base path
    if (canonicalUploadedFilePath.startsWith(canonicalExpectedLessonBasePath)) {
        return success(this).build();
    }
    return failed(this)
        .attemptWasMade()
        .feedback("path-traversal-profile-attempt")
        .feedbackArgs(canonicalUploadedFilePath.toString())
        .build();
  }

  public ResponseEntity<?> getProfilePicture(@CurrentUsername String username) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(MediaType.IMAGE_JPEG_VALUE))
        .body(getProfilePictureAsBase64(username));
  }

  protected byte[] getProfilePictureAsBase64(String username) {
    // Sanitize username
    String sanitizedUsername = sanitizeInput(username);

    Path baseUploadPath = Paths.get(this.webGoatHomeDirectory, UPLOAD_SUBDIRECTORY);
    // Ensure base directory exists before resolving user path
    try {
      Files.createDirectories(baseUploadPath);
    } catch (IOException e) {
      // Log error, but return default image if base path cannot be created/accessed
      return defaultImage();
    }

    Path userProfileDirectoryPath = baseUploadPath.resolve(sanitizedUsername);

    File profilePictureDirectory;
    try {
        Path canonicalBaseUploadPath = baseUploadPath.toRealPath();
        // Ensure userProfileDirectoryPath exists before calling toRealPath() on it
        Files.createDirectories(userProfileDirectoryPath);
        Path canonicalUserProfileDirectoryPath = userProfileDirectoryPath.toRealPath();

        if (!canonicalUserProfileDirectoryPath.startsWith(canonicalBaseUploadPath)) {
            // Path traversal attempt detected during profile picture retrieval
            return defaultImage(); // Return default image instead of exposing arbitrary files
        }
        profilePictureDirectory = canonicalUserProfileDirectoryPath.toFile();
    } catch (IOException e) {
        return defaultImage(); // Handle error or non-existent path gracefully
    }

    var profileDirectoryFiles = profilePictureDirectory.listFiles();

    if (profileDirectoryFiles != null && profileDirectoryFiles.length > 0) {
      return Arrays.stream(profileDirectoryFiles)
          .filter(file -> {
            try {
                // Validate each file's canonical path to ensure it's within the user's directory
                Path canonicalFile = file.toPath().toRealPath();
                return canonicalFile.startsWith(profilePictureDirectory.toPath().toRealPath()) &&
                       FilenameUtils.isExtension(file.getName(), List.of("jpg", "png"));
            } catch (IOException e) {
                return false; // Treat as invalid if path cannot be resolved or is outside
            }
          })
          .findFirst()
          .map(
              file -> {
                try (var inputStream = new FileInputStream(file)) { // Use the validated 'file'
                  return Base64.getEncoder().encode(FileCopyUtils.copyToByteArray(inputStream));
                } catch (IOException e) {
                  return defaultImage();
                }
              })
          .orElse(defaultImage());
    } else {
      return defaultImage();
    }
  }

  @SneakyThrows
  protected byte[] defaultImage() {
    var inputStream = getClass().getResourceAsStream("/images/account.png");
    return Base64.getEncoder().encode(FileCopyUtils.copyToByteArray(inputStream));
  }

  /**
   * Sanitizes input strings to prevent path traversal.
   * Allows alphanumeric characters, dots, hyphens, and underscores.
   *
   * @param input The string to sanitize (e.g., username, filename).
   * @return A sanitized string.
   */
  private String sanitizeInput(String input) {
    if (input == null) {
      return "";
    }
    // Remove any characters not matching the safe filename pattern
    return SAFE_FILENAME_PATTERN.matcher(input).replaceAll("");
  }
}
