package com.example.stream.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMpegStreamConverter {

    public String startFFmpegStream(String inputStreamUrl, String channelName, String token) {
        String hlsUrl = "http://localhost:8080/" + channelName + ".m3u8?token=" + token;

        List<String> command = new ArrayList<>();
        // Example command for converting stream to HLS (.m3u8)
        command.add("ffmpeg");
        command.add("-i");  // Input
        command.add(inputStreamUrl);  // Stream URL
        command.add("-c:v");  // Video codec
        command.add("libx264");
        command.add("-c:a");  // Audio codec
        command.add("aac");
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("10");  // Each segment of 10 seconds
        command.add("-hls_list_size");
        command.add("0");
        command.add("-hls_playlist_type");
        command.add("event");  // Continuous live stream type
        command.add("-hls_segment_filename");
        command.add("http://localhost:8080/" + channelName + "_%03d.ts");
        command.add(hlsUrl);  // stream server
        convertStreamToHLS(command);
        return hlsUrl;
    }

    public static void convertStreamToHLS(List<String> command) {

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge error stream with output stream

        try {
            Process process = processBuilder.start();

            // Capture the output and error streams
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern progressPattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)"); // Matches time format in ffmpeg output
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Print the output from ffmpeg
                    Matcher matcher = progressPattern.matcher(line);
                    if (matcher.find()) {
                        String hours = matcher.group(1);
                        String minutes = matcher.group(2);
                        String seconds = matcher.group(3);
                        System.out.println("Progress: " + hours + ":" + minutes + ":" + seconds);
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            // Check if the process completed successfully
            if (exitCode == 0) {
                System.out.println("FFMpeg conversion completed successfully.");
            } else {
                System.out.println("FFMpeg conversion failed with exit code: " + exitCode);
            }
        } catch (IOException e) {
            System.out.println("Error occurred while running ffmpeg: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Process interrupted: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
