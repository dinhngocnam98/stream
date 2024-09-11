package com.example.stream;

import java.util.Base64;
import java.util.Date;


public class TokenGenerator {

    // Secret key for token generation and validation (should be kept private)
    private static final String SECRET_KEY = "Chungt@l@@nhem!@#QWEasdzxc";

    public static String generateToken() {
        long timestamp = new Date().getTime();
        String data = "token-" + timestamp;
        String token = Base64.getEncoder().encodeToString((data + ":" + SECRET_KEY).getBytes());
        return token;
    }

    public static boolean validateToken(String token) {
        try {
            String decodedToken = new String(Base64.getDecoder().decode(token));
            String[] parts = decodedToken.split(":");
            if (parts.length != 2) {
                return false;
            }
            String data = parts[0];
            String secret = parts[1];

            // Check if the secret key matches
            if (!SECRET_KEY.equals(secret)) {
                return false;
            }

            // Check if the timestamp is valid (e.g., within the last 5 minutes)
            long timestamp = Long.parseLong(data.split("-")[1]);
            long currentTime = new Date().getTime();
            long difference = currentTime - timestamp;

            return difference <= 5 * 60 * 1000; // 5 minutes
        } catch (Exception e) {
            return false;
        }
    }
}
