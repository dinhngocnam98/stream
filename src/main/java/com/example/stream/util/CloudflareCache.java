package com.example.stream.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CloudflareCache {
    private static final String API_TOKEN = "your_cloudflare_api_token";  // Replace with your Cloudflare API token
    private static final String EMAIL = "your_email";                     // Replace with your Cloudflare account email
    private static final String ZONE_ID = "your_zone_id";                 // Replace with your Cloudflare Zone ID

    public static void cacheURL(String urlToCache) throws Exception {
        String apiURL = "https://api.cloudflare.com/client/v4/zones/" + ZONE_ID + "/purge_cache";

        // Prepare the JSON request body
        String jsonBody = "{\"files\":[\"" + urlToCache + "\"]}";

        // Create and configure the connection
        URL url = new URL(apiURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Auth-Email", EMAIL);
        conn.setDoOutput(true);

        // Send the request body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Check the response
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            System.out.println("URL cached successfully: " + urlToCache);
        } else {
            throw new Exception("Failed to cache URL. Response code: " + conn.getResponseCode());
        }
    }
}
