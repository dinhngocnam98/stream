package com.example.stream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLExtractor {
    public static String extractURL(String input) {
        String regex = "(http://[\\w\\.-]+:[0-9]+/[\\w]+/[\\w]+/[\\w]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String streamUrl = matcher.group(1);
            System.out.println("Extracted Stream URL: " + streamUrl);
            return streamUrl;
        } else {
            System.out.println("No stream URL found!");
            return null;
        }
    }
}
