package org.owasp.webgoat.lessons.cryptography;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class HashingAssignmentTest {

    private final HashingAssignment assignment = new HashingAssignment();

    @Test
    void getMd5_shouldStoreSecretFromDefinedSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);

        String md5Hash = assignment.getMd5(request);

        String secret = (String) session.getAttribute("md5Secret");
        assertThat(secret).isNotNull();
        assertThat(HashingAssignment.SECRETS).contains(secret);

        String recomputed =
            DatatypeConverter.printHexBinary(
                java.security.MessageDigest.getInstance("MD5").digest(secret.getBytes()))
                .toUpperCase();
        assertThat(md5Hash).isEqualTo(recomputed);
    }

    @Test
    void getSha256_shouldStoreSecretFromDefinedSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);

        String sha256 = assignment.getSha256(request);

        String secret = (String) session.getAttribute("sha256Secret");
        assertThat(secret).isNotNull();
        assertThat(HashingAssignment.SECRETS).contains(secret);

        String recomputed =
            HashingAssignment.getHash(secret, "SHA-256");
        assertThat(sha256).isEqualTo(recomputed);
    }

    @Test
    void getMd5_shouldProduceVarietyOfSecretsOverManyCalls() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Set<String> seenSecrets = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            request.getSession(true).invalidate();
            request = new MockHttpServletRequest();
            assignment.getMd5(request);
            String secret = (String) request.getSession().getAttribute("md5Secret");
            seenSecrets.add(secret);
        }

        assertThat(seenSecrets.size()).isGreaterThan(1);
    }
}
