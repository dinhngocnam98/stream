package com.example.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FFMpegStreamConverter {

    private static final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();

    public static Process startStream(String inputStreamUrl, String outputDirectory, String channelName) {
        String channelNameFormat = channelName.replace("|US|", "").trim().replaceAll(" ", "_");
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");  // Input
        command.add(inputStreamUrl);  // Stream URL
        command.add("-hide_banner");
        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("1");
        command.add("-hls_list_size");
        command.add("10");
        command.add("-hls_flags");
        command.add("delete_segments+split_by_time");
//        command.add("-hls_playlist_type");
//        command.add("event");
        command.add(outputDirectory + "/" + channelNameFormat + ".m3u8");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // Read the output and error streams in a separate thread
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
////                        System.out.println(line);
//                    }
                } catch (IOException e) {
                    System.out.println("Error reading FFmpeg output: " + e.getMessage());
                }
            }).start();

            processMap.put(channelName, process);
            return process;

        } catch (IOException e) {
            System.out.println("Error occurred while running ffmpeg: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> listProcessChannel() {
        return new ArrayList<>(processMap.keySet());
    }

    public static boolean channelExists(String channelName) {
        return processMap.containsKey(channelName);
    }

    public static boolean stopStream(String channelName) {
        Process process = processMap.get(channelName);
        if (process != null && process.isAlive()) {
            process.destroy();
            processMap.remove(channelName);
            return true;
        } else {
            return false;
        }
    }

    public static void stopAllStreams() {
        for (Map.Entry<String, Process> entry : processMap.entrySet()) {
            Process process = entry.getValue();
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
        processMap.clear();
    }
}
