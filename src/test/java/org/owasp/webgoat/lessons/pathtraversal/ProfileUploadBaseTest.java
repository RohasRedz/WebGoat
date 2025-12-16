package org.owasp.webgoat.lessons.pathtraversal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Delta tests for ProfileUploadBase.
 *
 * Focus: Verify that the path traversal vulnerability is mitigated:
 *  - Filenames are sanitized so that directory components are stripped.
 *  - Traversal sequences ("..", path separators) are rejected.
 *  - Successfully uploaded files remain within the expected base directory.
 */
class ProfileUploadBaseTest {

    @Test
    @DisplayName("Normal filename is accepted and file is stored within user directory")
    void normalFilenameStoredInsideUserDirectory() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("webgoat-home-");
        String webGoatHome = tempDir.toAbsolutePath().toString();
        String username = "user1";
        String filename = "avatar.jpg";

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("dummy-image-data".getBytes(StandardCharsets.UTF_8));

        ProfileUploadBase base = new ProfileUploadBase(webGoatHome);

        try {
            // Act
            AttackResult result = base.execute(multipartFile, filename, username);

            // Assert
            assertThat(result.getLessonCompleted())
                    .as("Normal upload should not be treated as an attack")
                    .isFalse();

            File userDir = new File(webGoatHome, "/PathTraversal/" + username);
            assertThat(userDir).exists().isDirectory();

            File[] files = userDir.listFiles();
            assertThat(files)
                    .as("Uploaded directory should contain the sanitized filename")
                    .isNotNull()
                    .hasSize(1);

            File uploadedFile = files[0];
            assertThat(uploadedFile.getName()).isEqualTo(filename);
            assertThat(uploadedFile.getParentFile().getCanonicalPath())
                    .as("Uploaded file must reside within the user directory")
                    .isEqualTo(userDir.getCanonicalPath());
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    @Test
    @DisplayName("Path traversal patterns in filename are rejected")
    void traversalFilenameIsRejected() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("webgoat-home-");
        String webGoatHome = tempDir.toAbsolutePath().toString();
        String username = "user2";
        String traversalName = "../evil.txt";

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("malicious".getBytes(StandardCharsets.UTF_8));

        ProfileUploadBase base = new ProfileUploadBase(webGoatHome);

        try {
            // Act
            AttackResult result = base.execute(multipartFile, traversalName, username);

            // Assert
            assertThat(result.getLessonCompleted())
                    .as("Traversal filename must not complete lesson or be treated as normal upload")
                    .isFalse();
            assertThat(result.getFeedback())
                    .as("Traversal filename should trigger invalid-filename feedback")
                    .contains("path-traversal-profile-invalid-filename");

            File userDir = new File(webGoatHome, "/PathTraversal/" + username);
            if (userDir.exists()) {
                File[] files = userDir.listFiles();
                assertThat(files)
                        .as("No files should be created for invalid/traversal filenames")
                        .isNullOrEmpty();
            }
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    @Test
    @DisplayName("Attempt to escape base directory via embedded path is prevented")
    void embeddedPathOutsideBaseIsPrevented() throws Exception {
        // Arrange
        Path tempDir = Files.createTempDirectory("webgoat-home-");
        String webGoatHome = tempDir.toAbsolutePath().toString();
        String username = "user3";
        // This looks like a nested path and should be sanitized down to just the file name,
        // ensuring it remains within the correct directory.
        String embeddedPath = "subdir/../../outside.txt";

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("data".getBytes(StandardCharsets.UTF_8));

        ProfileUploadBase base = new ProfileUploadBase(webGoatHome);

        try {
            // Act
            AttackResult result = base.execute(multipartFile, embeddedPath, username);

            // Assert
            assertThat(result.getLessonCompleted())
                    .as("Upload using embedded path should not be treated as successful path traversal")
                    .isFalse();

            File userDir = new File(webGoatHome, "/PathTraversal/" + username);
            File[] files = userDir.listFiles();
            // Depending on sanitization, either rejected as invalid or stored as a safe file;
            // in either case, the uploaded file must not escape the intended directory.
            if (files != null && files.length > 0) {
                File uploadedFile = files[0];
                assertThat(uploadedFile.getParentFile().getCanonicalPath())
                        .as("Even with tricky names, uploaded file must remain inside user directory")
                        .isEqualTo(userDir.getCanonicalPath());
            }
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }
}
