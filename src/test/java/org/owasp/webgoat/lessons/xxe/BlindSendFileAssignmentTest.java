package org.owasp.webgoat.lessons.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.WebGoatUser;

public class BlindSendFileAssignmentTest {

    @Test
    void initialize_shouldCreateSecretFileWithinUserDirectoryEvenWithTraversalUsername() throws Exception {
        Path tempDir = Files.createTempDirectory("webgoat-xxe-test");
        String baseDir = tempDir.toAbsolutePath().toString();

        CommentsCache commentsCache = new CommentsCache();
        BlindSendFileAssignment assignment =
            new BlindSendFileAssignment(baseDir, commentsCache);

        WebGoatUser maliciousUser = new WebGoatUser("attacker/../../evil");

        assignment.initialize(maliciousUser);

        File xxeBase = new File(baseDir, "XXE");
        assertThat(xxeBase.exists()).isTrue();
        File[] subDirs = xxeBase.listFiles();
        boolean foundSecret = false;
        if (subDirs != null) {
            for (File dir : subDirs) {
                File secret = new File(dir, "secret.txt");
                if (secret.exists()) {
                    assertThat(secret.getCanonicalPath())
                        .startsWith(xxeBase.getCanonicalPath());
                    foundSecret = true;
                }
            }
        }
        assertThat(foundSecret).isTrue();
    }
}
