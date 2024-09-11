package com.example.stream.util;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class ChanelNameExtractor {

    // Regular expression to capture the tvg-name or similar description of the channel
    private static final String GENERAL_REGEX = "tvg-name=\"[^\"]+\"|,\\|US\\|\\s*[A-Za-z0-9\\s]+";

    public String extractChannelName(String content) {
        Pattern pattern = Pattern.compile(GENERAL_REGEX);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String match = matcher.group();

            // Clean the extracted value to get only the channel name
            return cleanChannelName(match);
        }

        return null;  // Return null if no match is found
    }

    // Method to clean and extract channel name from the matched string
    private String cleanChannelName(String match) {
        // Remove 'tvg-name="' or ',"|US| ' parts from the match
        if (match.contains("tvg-name")) {
            match = match.replaceAll("tvg-name=\"", "").replaceAll("\"", "");
        } else if (match.contains(",|US|")) {
            match = match.replace(",|US|", "").trim();
        }

        return match.trim();
    }
}
