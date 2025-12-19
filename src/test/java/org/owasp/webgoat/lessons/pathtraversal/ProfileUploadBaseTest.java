package org.owasp.webgoat.lessons.pathtraversal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.mock.web.MockMultipartFile;

public class ProfileUploadBaseTest {

    @Test
    void execute_shouldSanitizeFilenameAndKeepFileInsideUserDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("webgoat-pathtraversal-test");
        String baseDir = tempDir.toAbsolutePath().toString();
        ProfileUploadBase base = new ProfileUploadBase(baseDir);

        byte[] content = "image".getBytes();
        MockMultipartFile file =
            new MockMultipartFile("file", "evil/../../outside.jpg", "image/jpeg", content);

        String username = "user1";
        String fullName = "evil/../../outside.jpg";

        AttackResult result = base.execute(file, fullName, username);

        File userDir = new File(baseDir, "/PathTraversal/" + username);
        assertThat(userDir.exists()).isTrue();
        File[] stored = userDir.listFiles();
        assertThat(stored).isNotNull();
        for (File f : stored) {
            assertThat(f.getCanonicalPath()).startsWith(userDir.getCanonicalPath());
        }
        assertThat(result).isNotNull();
    }

    @Test
    void execute_shouldDetectDirectTraversalAttemptAndFail() throws Exception {
        Path tempDir = Files.createTempDirectory("webgoat-pathtraversal-test2");
        String baseDir = tempDir.toAbsolutePath().toString();
        ProfileUploadBase base = new ProfileUploadBase(baseDir);

        byte[] content = "image".getBytes();
        MockMultipartFile file =
            new MockMultipartFile("file", "../outside.jpg", "image/jpeg", content);

        String username = "user2";
        String fullName = "../outside.jpg";

        AttackResult result = base.execute(file, fullName, username);

        assertThat(result.getLessonCompleted()).isFalse();
    }
}
