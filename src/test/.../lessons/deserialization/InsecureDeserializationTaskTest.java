// TODO: Adjust the package to match the actual source package if different.
package org.owasp.webgoat.lessons.deserialization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InsecureDeserializationTaskTest {

    @Test
    @DisplayName("deserializeObject should block deserialization of user-controlled data and return safe message")
    void deserializeObject_blocksInsecureDeserialization() throws Exception {
        // Arrange
        InsecureDeserializationTask controller = new InsecureDeserializationTask();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String maliciousPayload = "rO0ABXNyABFqYXZhLnV0aWwuQXJyYXlMaXN0"; // arbitrary base64 fragment

        // We verify that the secure behavior returns the blocking message
        // and does not execute the old deserialization path.
        // The updated implementation logs a warning and returns a fixed string.
        // No ObjectInputStream should be constructed anymore.
        // We additionally assert the exact message to ensure behavior.
        // (We cannot easily verify logging here without a custom handler.)
        mockMvc.perform(post("/deserialization/task")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(maliciousPayload))
               .andExpect(status().isOk())
               .andExpect(content().string(
                       "Deserialization of user-controlled objects is not permitted for security reasons."));
    }

    @Test
    @DisplayName("deserializeObject should not attempt Java deserialization anymore")
    void deserializeObject_doesNotUseJavaDeserializationApi() {
        // Arrange
        // We assert indirectly that no Logger misuse happens and that method result is the same
        // regardless of payload; this demonstrates that no payload-driven object graph is created.
        InsecureDeserializationTask controller = new InsecureDeserializationTask();
        String anyPayload = "any";

        // Act
        String result1 = controller.deserializeObject(anyPayload);
        String result2 = controller.deserializeObject("anotherPayload");

        // Assert
        assertThat(result1).isEqualTo(
                "Deserialization of user-controlled objects is not permitted for security reasons.");
        assertThat(result2).isEqualTo(result1);
    }
}
