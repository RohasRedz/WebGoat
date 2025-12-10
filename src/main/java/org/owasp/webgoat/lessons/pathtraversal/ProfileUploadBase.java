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
import java.nio.file.Path; // Added for Path operations
import java.nio.file.Paths; // Added for Path operations
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

  // Helper method to validate if a target path is within a base directory
  private boolean isPathWithinBase(File baseDir, File targetFile) throws IOException {
    // Get canonical paths to resolve any '..' or '.' components
    Path canonicalBasePath = baseDir.getCanonicalFile().toPath();
    Path canonicalTargetPath = targetFile.getCanonicalFile().toPath();

    // Check if the target path starts with the base path
    return canonicalTargetPath.startsWith(canonicalBasePath);
  }

  protected AttackResult execute(MultipartFile file, String fullName, String username) {
    if (file.isEmpty()) {
      return failed(this).feedback("path-traversal-profile-empty-file").build();
    }
    if (StringUtils.isEmpty(fullName)) {
      return failed(this).feedback("path-traversal-profile-empty-name").build();
    }

    // Sanitize username to prevent path traversal in directory creation
    // Allow alphanumeric, hyphen, underscore, and dot. Remove any other characters.
    String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9-_.]", "");
    if (sanitizedUsername.isEmpty()) {
        return failed(this).feedback("path-traversal-profile-invalid-username").build();
    }

    File uploadDirectory = cleanupAndCreateDirectoryForUser(sanitizedUsername);

    try {
      // Validate uploadDirectory is within webGoatHomeDirectory
      if (!isPathWithinBase(new File(this.webGoatHomeDirectory), uploadDirectory)) {
        return failed(this).feedback("path-traversal-profile-directory-escape").build();
      }

      // Sanitize fullName to prevent path traversal in filename
      // FilenameUtils.getName extracts the last component of a path, effectively removing directory separators
      String sanitizedFullName = FilenameUtils.getName(fullName);
      if (sanitizedFullName.isEmpty()) {
          return failed(this).feedback("path-traversal-profile-invalid-filename").build();
      }

      var uploadedFile = new File(uploadDirectory, sanitizedFullName);

      // Validate uploadedFile is within uploadDirectory
      if (!isPathWithinBase(uploadDirectory, uploadedFile)) {
        return failed(this).feedback("path-traversal-profile-file-escape").build();
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
    // username is already sanitized by the calling method (execute)
    var uploadDirectory = new File(this.webGoatHomeDirectory, "PathTraversal" + File.separator + username);
    if (uploadDirectory.exists()) {
      FileSystemUtils.deleteRecursively(uploadDirectory);
    }
    Files.createDirectories(uploadDirectory.toPath());
    return uploadDirectory;
  }

  private boolean attemptWasMade(File expectedUploadDirectory, File uploadedFile)
      throws IOException {
    // This check is still valid as it compares canonical paths, but the primary defense is now
    // in the isPathWithinBase checks before file creation.
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
    // Sanitize username to prevent path traversal in directory access
    String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9-_.]", "");
    if (sanitizedUsername.isEmpty()) {
        return defaultImage(); // Return default image for invalid username
    }

    var profilePictureDirectory = new File(this.webGoatHomeDirectory, "PathTraversal" + File.separator + sanitizedUsername);

    try {
      // Validate profilePictureDirectory is within webGoatHomeDirectory
      if (!isPathWithinBase(new File(this.webGoatHomeDirectory), profilePictureDirectory)) {
        return defaultImage(); // Return default image if directory escapes
      }
    } catch (IOException e) {
      // Log error if canonical path check fails, return default image
      return defaultImage();
    }

    var profileDirectoryFiles = profilePictureDirectory.listFiles();

    if (profileDirectoryFiles != null && profileDirectoryFiles.length > 0) {
      return Arrays.stream(profileDirectoryFiles)
          .filter(file -> FilenameUtils.isExtension(file.getName(), List.of("jpg", "png")))
          .findFirst()
          .map(
              file -> { // Fix: use 'file' from the stream, not profileDirectoryFiles[0]
                try (var inputStream = new FileInputStream(file)) {
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
