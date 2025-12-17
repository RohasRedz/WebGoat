package org.owasp.webgoat.lessons.deserialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputFilter;
import java.util.Set;
import java.util.HashSet;

public class InsecureDeserializationTask {

    // Define a whitelist of allowed classes for deserialization
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>();

    static {
        // Add classes that are explicitly allowed to be deserialized
        ALLOWED_CLASSES.add("java.lang.String");
        ALLOWED_CLASSES.add("java.util.ArrayList");
        // Add other trusted classes as needed
    }

    // Original vulnerable method (simulated based on vulnerability description)
    // public Object vulnerableMethod(byte[] serializedData) throws IOException, ClassNotFoundException {
    //     try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedData))) {
    //         return ois.readObject(); // L45
    //     }
    // }

    public Object safeDeserialize(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            // Apply a serialization filter to restrict allowed classes (JEP 290)
            ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                (info) -> {
                    if (info.serialClass() != null) {
                        if (ALLOWED_CLASSES.contains(info.serialClass().getName())) {
                            return ObjectInputFilter.Status.ALLOWED;
                        }
                        return ObjectInputFilter.Status.REJECTED;
                    }
                    return ObjectInputFilter.Status.UNDECIDED;
                }
            ));

            return ois.readObject(); // L45 - Fixed: Deserialization with filter
        }
    }
}
