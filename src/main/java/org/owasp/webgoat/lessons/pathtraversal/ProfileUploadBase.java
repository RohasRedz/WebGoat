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
    if (StringUtils.isEmpty(fullName)) {
      return failed(this).feedback("path-traversal-profile-empty-name").build();
    }

    File uploadDirectory = cleanupAndCreateDirectoryForUser(username);

    try {
      // Remediation: Sanitize fullName to prevent path traversal and ensure file is within intended directory.
      // Use FilenameUtils.getName() to get just the filename, preventing directory manipulation.
      String safeFileName = FilenameUtils.getName(fullName);
      Path resolvedPath = Paths.get(uploadDirectory.getAbsolutePath(), safeFileName).normalize();

      // Ensure the resolved path is still within the intended upload directory
      if (!resolvedPath.startsWith(uploadDirectory.toPath())) {
        return failed(this).feedback("path-traversal-attempt-detected").build();
      }

      var uploadedFile = resolvedPath.toFile();
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
    // Remediation: Ensure username cannot cause path traversal when creating directory
    // by sanitizing it or ensuring it's just a simple name.
    // For this context, assuming username is already sanitized or validated to be a simple name.
    Path userSpecificPath = Paths.get(this.webGoatHomeDirectory, "PathTraversal", username).normalize();
    if (!userSpecificPath.startsWith(Paths.get(this.webGoatHomeDirectory, "PathTraversal"))) {
        throw new IOException("Attempted path traversal in username for directory creation.");
    }

    var uploadDirectory = userSpecificPath.toFile();
    if (uploadDirectory.exists()) {
      FileSystemUtils.deleteRecursively(uploadDirectory);
    }
    Files.createDirectories(uploadDirectory.toPath());
    return uploadDirectory;
  }

  private boolean attemptWasMade(File expectedUploadDirectory, File uploadedFile)
      throws IOException {
    // Remediation: Use canonical paths for comparison to prevent path traversal bypasses
    return !expectedUploadDirectory
        .getCanonicalFile()
        .equals(uploadedFile.getParentFile().getCanonicalFile());
  }

  private AttackResult solvedIt(File uploadedFile) throws IOException {
    // Remediation: Use canonical path for comparison
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
    // Remediation: Ensure username cannot cause path traversal when accessing directory
    Path profilePicturePath = Paths.get(this.webGoatHomeDirectory, "PathTraversal", username).normalize();
    if (!profilePicturePath.startsWith(Paths.get(this.webGoatHomeDirectory, "PathTraversal"))) {
        return defaultImage(); // Fallback to default image on traversal attempt
    }
    var profilePictureDirectory = profilePicturePath.toFile();
    var profileDirectoryFiles = profilePictureDirectory.listFiles();

    if (profileDirectoryFiles != null && profileDirectoryFiles.length > 0) {
      return Arrays.stream(profileDirectoryFiles)
          .filter(file -> FilenameUtils.isExtension(file.getName(), List.of("jpg", "png")) && !file.getName().contains("..")) // Added check for ".." in filename
          .findFirst()
          .map(
              file -> {
                try (var inputStream = new FileInputStream(file)) { // Use 'file' directly
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
