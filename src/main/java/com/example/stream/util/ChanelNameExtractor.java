package com.example.stream.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChanelNameExtractor {

    // Regular expression to capture the tvg-name or similar description of the channel
    private static final String GENERAL_REGEX_CHANNEL_NAME = "tvg-name=\"([^\"]*)\".*?,\\s*([^\\n]+)";
    private static final String GENERAL_REGEX_GROUP_TITLE = "group-title=\"([^\"]*)\".*?,\\s*([^\\n]+)";
    private static final String GENERAL_REGEX_LOGO = "tvg-logo=\"([^\"]+)\"";
    private static final String GENERAL_REGEX_STREAM_URL = "\\?url=(.+)";

    public String extractGroup(String content) {
        Pattern pattern = Pattern.compile(GENERAL_REGEX_GROUP_TITLE);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String match = matcher.group(1).trim();
            // Clean the extracted value to get only the channel name
            return clean(match);
        }
        return null;  // Return null if no match is found
    }

    public String extractChannelName(String content) {
        Pattern pattern = Pattern.compile(GENERAL_REGEX_GROUP_TITLE);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String[] arrayContent= content.trim().split(",");
            String channelName = arrayContent[arrayContent.length - 1].trim();
            // Clean the extracted value to get only the channel name
            return clean(channelName);
        }
        return null;  // Return null if no match is found
    }

    public String extractLogo(String content) {
        Pattern pattern = Pattern.compile(GENERAL_REGEX_LOGO);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String match = matcher.group(1).trim();
            // Clean the extracted value to get only the channel name
            return clean(match);
        }

        return null;  // Return null if no match is found
    }

    public String extractStreamUrl(String content) {
        Pattern pattern = Pattern.compile(GENERAL_REGEX_STREAM_URL);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            // Group 1 là URL sau ?url= (giữ lại mọi thứ sau nó)
            String url = matcher.group(1).trim();

            try {
                // Giải mã URL
                return URLDecoder.decode(url, StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace(); // In ra lỗi nếu có
            }
        }
        return null;  // Trả về null nếu không tìm thấy khớp
    }


    // Method to clean and extract channel name from the matched string
    private String clean(String match) {
        // Remove 'tvg-name="' or ',"|US| ' parts from the match
        if (match.contains("tvg-name")) {
            match = match.replaceAll("tvg-name=\"", "").replaceAll("\"", "");
        } else if (match.contains("tvg-logo")) {
            match = match.replaceAll("tvg-logo=\"", "").replaceAll("\"", "");

        } else if (match.contains("group-title")) {
            match = match.replaceAll("group-title=\"", "").replaceAll("\"", "");

        } else if (match.contains(",|US|")) {
            match = match.replace(",|US|", "").trim();
        }

        return match.trim();
    }
}
