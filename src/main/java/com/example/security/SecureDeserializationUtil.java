package com.example.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecureDeserializationUtil {

    // Whitelist of allowed classes for deserialization
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>(Arrays.asList(
            "java.lang.String",
            "java.lang.Integer",
            "java.util.ArrayList",
            "com.example.security.MySafeObject" // Assuming this is a safe class
    ));

    public static Object deserialize(InputStream is) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ValidatingObjectInputStream(is, ALLOWED_CLASSES)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Log the exception for debugging, but avoid exposing sensitive details
            // For production, use a proper logging framework like SLF4J/Logback
            System.err.println("Deserialization failed: " + e.getMessage());
            throw e;
        }
    }

    // Custom ObjectInputStream to enforce the whitelist
    private static class ValidatingObjectInputStream extends ObjectInputStream {
        private final Set<String> whitelist;

        public ValidatingObjectInputStream(InputStream in, Set<String> whitelist) throws IOException {
            super(in);
            this.whitelist = whitelist;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String className = desc.getName();
            if (!whitelist.contains(className)) {
                System.err.println("Attempted to deserialize unauthorized class: " + className);
                throw new InvalidClassException("Unauthorized deserialization attempt", className);
            }
            return super.resolveClass(desc);
        }
    }

    // Example of a safe serializable class
    static class MySafeObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String data;
        private int value;

        public MySafeObject(String data, int value) {
            this.data = data;
            this.value = value;
        }

        public String getData() {
            return data;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "MySafeObject{" +
                   "data='" + data + '\'' +
                   ", value=" + value +
                   '}';
        }
    }
}