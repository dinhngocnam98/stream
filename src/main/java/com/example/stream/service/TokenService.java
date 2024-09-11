package com.example.stream.service;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Date;
public class TokenService {

    private static final String SECRET_KEY = "T@iKh@ngbiet!@#QWEasdfZXCV123456789";

    public String generateToken() {
        long timestamp = new Date().getTime();
        String data = "token-" + timestamp;
        return Base64.getEncoder().encodeToString((data + ":" + SECRET_KEY).getBytes());
    }

    public boolean validateToken(String token) {
        try {
            String decodedToken = new String(Base64.getDecoder().decode(token));
            String[] parts = decodedToken.split(":");
            if (parts.length != 2) return false;

            String data = parts[0];
            String secret = parts[1];
            long timestamp = Long.parseLong(data.split("-")[1]);
            long currentTime = new Date().getTime();

            // Validate secret and ensure token is not older than 5 minutes
            return SECRET_KEY.equals(secret) && (currentTime - timestamp <= 5 * 60 * 1000);
        } catch (Exception e) {
            return false;
        }
    }
}
