package org.owasp.webgoat.lessons.deserialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ObjectInputFilter; // Added import

public class InsecureDeserializationTask {

    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            // Apply a serialization filter to restrict allowed classes (JEP 290)
            ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                "org.owasp.webgoat.lessons.deserialization.AllowedClass;!*" // Example: allow only 'AllowedClass' from this package
            );
            ois.setObjectInputFilter(filter); // Applied filter
            return ois.readObject();
        }
    }

    // Example of an allowed class (hypothetical)
    static class AllowedClass implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
    }
}
