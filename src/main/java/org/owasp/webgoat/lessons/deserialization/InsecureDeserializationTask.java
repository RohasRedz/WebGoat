/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.nio.charset.StandardCharsets; // REMEDIATION: Added for consistent character encoding
import java.security.MessageDigest; // REMEDIATION: Added for constant-time comparison
import java.util.Base64;
import java.util.regex.Matcher; // REMEDIATION: Added for basic JSON parsing
import java.util.regex.Pattern; // REMEDIATION: Added for basic JSON parsing
import javax.crypto.Mac; // REMEDIATION: Added for HMAC calculation
import javax.crypto.spec.SecretKeySpec; // REMEDIATION: Added for HMAC calculation
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({
  "insecure-deserialization.hints.1",
  "insecure-deserialization.hints.2",
  "insecure-deserialization.hints.3"
})
public class InsecureDeserializationTask implements AssignmentEndpoint {

  // REMEDIATION: Define a secure, secret key for HMAC. In a real application, this would be loaded
  // from a secure configuration management system (e.g., environment variable, Vault).
  private static final byte[] HMAC_SECRET_KEY_BYTES =
      "superSecretKeyForWebGoatLesson".getBytes(StandardCharsets.UTF_8);
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  @PostMapping("/InsecureDeserialization/task")
  @ResponseBody
  public AttackResult completed(@RequestParam String token) throws IOException {
    String b64token;

    b64token = token.replace('-', '+').replace('_', '/');

    try {
      byte[] decodedBytes = Base64.getDecoder().decode(b64token);
      String jsonPayload = new String(decodedBytes, StandardCharsets.UTF_8);

      // REMEDIATION: Safely parse JSON payload using regex (simplified for this exercise).
      // In a real application, use a robust JSON library like Jackson or Gson for parsing.
      Pattern taskPattern = Pattern.compile("\"task\":\"([^\"]*)\"");
      Pattern expPattern = Pattern.compile("\"exp\":(\\d+)");
      Pattern sigPattern = Pattern.compile("\"sig\":\"([^\"]*)\"");

      Matcher taskMatcher = taskPattern.matcher(jsonPayload);
      Matcher expMatcher = expPattern.matcher(jsonPayload);
      Matcher sigMatcher = sigPattern.matcher(jsonPayload);

      String task = null;
      long expiration = -1;
      String receivedSig = null;

      if (taskMatcher.find()) {
        task = taskMatcher.group(1);
      }
      if (expMatcher.find()) {
        expiration = Long.parseLong(expMatcher.group(1));
      }
      if (sigMatcher.find()) {
        receivedSig = sigMatcher.group(1);
      }

      if (task == null || expiration == -1 || receivedSig == null) {
        return failed(this).feedback("insecure-deserialization.invalid_token_format").build();
      }

      // REMEDIATION: Validate expiration timestamp to prevent replay attacks with old tokens.
      if (System.currentTimeMillis() > expiration) {
        return failed(this).feedback("insecure-deserialization.token_expired").build();
      }

      // REMEDIATION: Recompute HMAC signature and compare securely using constant-time comparison.
      Mac hmacSha256 = Mac.getInstance(HMAC_ALGORITHM);
      SecretKeySpec secretKey = new SecretKeySpec(HMAC_SECRET_KEY_BYTES, HMAC_ALGORITHM);
      hmacSha256.init(secretKey);

      // The data that was originally signed to create the token's signature
      String dataToSign = task + expiration;
      byte[] computedSigBytes = hmacSha256.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
      byte[] receivedSigBytes = Base64.getDecoder().decode(receivedSig);

      // Use constant-time comparison to prevent timing attacks
      if (!MessageDigest.isEqual(computedSigBytes, receivedSigBytes)) {
        return failed(this).feedback("insecure-deserialization.invalid_signature").build();
      }

      // REMEDIATION: If all checks pass, the token is considered valid and processed securely.
      // The original lesson's timing logic is replaced with secure token validation.
      return success(this).feedback("insecure-deserialization.secure_token_processed").build();

    } catch (IllegalArgumentException e) {
      // Catches errors related to malformed Base64 strings
      return failed(this).feedback("insecure-deserialization.invalid_base64").build();
    } catch (Exception e) {
      // Catches any other parsing, cryptographic, or runtime exceptions
      return failed(this).feedback("insecure-deserialization.token_processing_error").build();
    }
  }
}
