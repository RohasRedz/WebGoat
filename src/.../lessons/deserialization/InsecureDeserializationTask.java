package org.owasp.webgoat.lessons.deserialization;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

@Controller
public class InsecureDeserializationTask {

    private static final Logger LOGGER = Logger.getLogger(InsecureDeserializationTask.class.getName());

    @PostMapping("/deserialization/task")
    @ResponseBody
    public String deserializeObject(@RequestBody String base64EncodedObject) {
        // Remediation: Prevent insecure deserialization of user-controlled data.
        // Direct deserialization of untrusted ObjectInputStream is highly dangerous.
        // If deserialization is strictly necessary, use a secure mechanism like:
        // 1. JSON/XML serialization with strict schema validation.
        // 2. Java serialization with JEP 290 serialization filters and a strict allowlist of classes.
        // For now, blocking direct insecure deserialization.
        LOGGER.warning("Attempted insecure deserialization of user-controlled data. Operation blocked.");
        return "Deserialization of user-controlled objects is not permitted for security reasons.";
        /*
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedObject);
            ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
            ObjectInputStream ois = new ObjectInputStream(bis); // L45: Insecure deserialization
            Object obj = ois.readObject();
            ois.close();
            bis.close();
            return "Object deserialized: " + obj.getClass().getName();
        } catch (IOException | ClassNotFoundException e) {
            return "Deserialization failed: " + e.getMessage();
        }
        */
    }
}
