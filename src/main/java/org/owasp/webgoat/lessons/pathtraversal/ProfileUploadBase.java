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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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

  private final String webGoatHomeDirectory;

  public ProfileUploadBase(String webGoatHomeDirectory) {
    this.webGoatHomeDirectory = webGoatHomeDirectory;
  }

  protected AttackResult execute(MultipartFile file, String fullName, String username) {
    if (file.isEmpty()) {
      return failed(this).feedback("path-traversal-profile-empty-file").build();
    }
    if (!StringUtils.hasText(fullName)) { // Use hasText for better validation
      return failed(this).feedback("path-traversal-profile-empty-name").build();
    }

    // Remediation: Sanitize fullName to prevent path traversal
    String sanitizedFullName = FilenameUtils.getName(fullName); // Get just the filename, remove path info
    if (!StringUtils.hasText(sanitizedFullName)) {
        return failed(this).feedback("path-traversal-profile-invalid-filename").build();
    }

    File uploadDirectory = cleanupAndCreateDirectoryForUser(username);
    if (uploadDirectory == null) { // Check if directory creation failed due to invalid username
        return failed(this).feedback("path-traversal-profile-invalid-username-dir").build();
    }

    try {
      File uploadedFile = new File(uploadDirectory, sanitizedFullName);
      // Remediation: Canonical path check before writing
      String canonicalUploadPath = uploadDirectory.getCanonicalPath();
      String canonicalTargetFilePath = uploadedFile.getCanonicalPath();

      if (!canonicalTargetFilePath.startsWith(canonicalUploadPath + File.separator)) {
          // If the canonical path of the target file does not start with the canonical path
          // of the intended directory, it's a path traversal attempt.
          return failed(this).feedback("path-traversal-profile-attempt-write").feedbackArgs(canonicalTargetFilePath).build();
      }

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
  protected File cleanupAndCreateDirectoryForUser(String username) {
    // Remediation: Sanitize username for directory creation
    String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9.-]", "_"); // Allow alphanumeric, dot, dash
    if (!StringUtils.hasText(sanitizedUsername)) {
        return null; // Indicate failure to create directory
    }

    File baseDir = new File(this.webGoatHomeDirectory);
    File uploadDirectory = new File(baseDir, "PathTraversal" + File.separator + sanitizedUsername);

    // Remediation: Canonical path check for the created directory
    String canonicalBaseDir = baseDir.getCanonicalPath();
    String canonicalUploadDir = uploadDirectory.getCanonicalPath();

    if (!canonicalUploadDir.startsWith(canonicalBaseDir + File.separator + "PathTraversal" + File.separator)) {
        // Log this as a severe error, should not happen with sanitized username
        return null;
    }

    if (uploadDirectory.exists()) {
      FileSystemUtils.deleteRecursively(uploadDirectory);
    }
    Files.createDirectories(uploadDirectory.toPath());
    return uploadDirectory;
  }

  private boolean attemptWasMade(File expectedUploadDirectory, File uploadedFile)
      throws IOException {
    return !expectedUploadDirectory
        .getCanonicalPath()
        .equals(uploadedFile.getParentFile().getCanonicalPath());
  }

  private AttackResult solvedIt(File uploadedFile) throws IOException {
    if (uploadedFile.getCanonicalFile().getParentFile().getName().endsWith("PathTraversal")) {
      return success(this).build();
    }
    return failed(this)
        .attemptWasMade()
        .feedback("path-traversal-profile-attempt")
        .feedbackArgs(uploadedFile.getCanonicalPath())
        .build();
  }

  public ResponseEntity<?> getProfilePicture(@CurrentUsername String username) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(MediaType.IMAGE_JPEG_VALUE))
        .body(getProfilePictureAsBase64(username));
  }

  protected byte[] getProfilePictureAsBase64(String username) {
    // Remediation: Sanitize username for directory access
    String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9.-]", "_");
    if (!StringUtils.hasText(sanitizedUsername)) {
        return defaultImage();
    }

    File baseDir = new File(this.webGoatHomeDirectory);
    File profilePictureDirectory = new File(baseDir, "PathTraversal" + File.separator + sanitizedUsername);

    // Remediation: Canonical path check for the accessed directory
    try {
        String canonicalBaseDir = baseDir.getCanonicalPath();
        String canonicalProfileDir = profilePictureDirectory.getCanonicalPath();

        if (!canonicalProfileDir.startsWith(canonicalBaseDir + File.separator + "PathTraversal" + File.separator)) {
            return defaultImage(); // Path traversal attempt detected
        }
    } catch (IOException e) {
        return defaultImage(); // Error getting canonical path
    }


    var profileDirectoryFiles = profilePictureDirectory.listFiles();

    if (profileDirectoryFiles != null && profileDirectoryFiles.length > 0) {
      return Arrays.stream(profileDirectoryFiles)
          .filter(file -> FilenameUtils.isExtension(file.getName(), List.of("jpg", "png")) && !file.getName().contains("..")) // Added check for .. in filename
          .findFirst()
          .map(
              file -> {
                try (var inputStream = new FileInputStream(file)) { // Changed from profileDirectoryFiles[0] to file
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
}
