public class UserAuthenticator {

    // private static final String ADMIN_PASSWORD = "adminPassword123"; // Removed hardcoded password
    private static final String ADMIN_PASSWORD_PLACEHOLDER = "RETRIEVE_FROM_SECURE_CONFIG"; // Placeholder for secure retrieval

    public boolean authenticate(String username, String password) {
        // Admin authentication should use a securely retrieved password, not a hardcoded one.
        // For demonstration, this will now fail unless the placeholder is used (which it shouldn't be).
        if ("admin".equals(username) && ADMIN_PASSWORD_PLACEHOLDER.equals(password)) {
            System.out.println("Admin authenticated successfully (using placeholder - FIX ME!).");
            return true;
        } else if ("user".equals(username) && "userPass".equals(password)) {
            System.out.println("User authenticated successfully.");
            return true;
        }
        System.out.println("Authentication failed.");
        return false;
    }

    public static void main(String[] args) {
        UserAuthenticator authenticator = new UserAuthenticator();
        // This call will now fail as ADMIN_PASSWORD_PLACEHOLDER is not "adminPassword123"
        authenticator.authenticate("admin", "adminPassword123");
        authenticator.authenticate("user", "userPass"); // This should still pass
        authenticator.authenticate("admin", "RETRIEVE_FROM_SECURE_CONFIG"); // This would pass if the placeholder is used
    }
}