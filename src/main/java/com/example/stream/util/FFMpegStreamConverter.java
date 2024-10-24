package com.example.stream.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FFMpegStreamConverter {
    private static final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();

    public static void startStream(String inputStreamUrl, String outputDirectory, String channelId, String channelName) {
        System.out.println("===============Starting FFMpegStreamConverter=============== :" + channelName + "(" + channelId + ")");

        File directory = new File(outputDirectory, channelId);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Directory created: " + directory.getAbsolutePath());
            } else {
                System.err.println("Failed to create directory: " + directory.getAbsolutePath());
                return; // Ngừng nếu không thể tạo thư mục
            }
        }

        List<String> command = buildFfmpegCommand(inputStreamUrl, outputDirectory, channelId);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);  // Kế thừa luồng output và error
        try {
            Process process = processBuilder.start();
            processMap.put(channelId, process);  // Lưu process vào map

            // Khởi chạy một thread để theo dõi tiến trình
            new Thread(() -> {
                try {
                    handleProcessOutput(process, outputDirectory, channelId, channelName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Error occurred while running FFmpeg: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> buildFfmpegCommand(String inputStreamUrl, String outputDirectory, String channelId) {
        return Arrays.asList(
                "ffmpeg", "-re",
                "-i", inputStreamUrl,  // Input livestream URL
                "-hide_banner", "-loglevel", "quiet",
                "-c", "copy",  // Copy video và audio mà không xử lý
                "-f", "hls", "-hls_time", "4",  // Giảm thời gian mỗi segment
                "-hls_list_size", "50",  // Giảm danh sách segment
                "-hls_flags", "delete_segments+append_list",  // Không xóa segment cũ
                "-tune", "zerolatency",  // Tối ưu hóa cho livestream độ trễ thấp
                outputDirectory + "/" + channelId + "/" + channelId + ".m3u8"  // Output file
        );
    }

    private static void handleProcessOutput(Process process, String outputDirectory, String channelId, String channelName) throws IOException {
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println(channelName + " (" + channelId + "): FFmpeg process failed with exit code " + exitCode);
            } else {
                System.err.println(channelName + " (" + channelId + "): Stream stopped");
            }
            cleanUpFiles(outputDirectory, channelId);  // Xóa file nếu có lỗi
            updateChannels(outputDirectory, channelName);
            processMap.remove(channelId);  // Xóa process khỏi map
        } catch (IOException | InterruptedException e) {
            System.err.println("Error reading FFmpeg output: " + e.getMessage());
            updateChannels(outputDirectory, channelName);
            Thread.currentThread().interrupt();
        }
    }

    private static void cleanUpFiles(String outputDirectory, String channelId) {
        Path directoryPath = Paths.get(outputDirectory, channelId);
        try {
            if (Files.exists(directoryPath)) {
                // Duyệt và xóa toàn bộ file trong thư mục và thư mục con
                Files.walk(directoryPath)
                        .sorted(Comparator.reverseOrder())  // Đảm bảo xóa file trước rồi mới xóa thư mục
                        .forEach(path -> {
                            try {
                                Files.delete(path);  // Xóa file hoặc thư mục
                            } catch (IOException e) {
                                System.err.println("Error deleting file/directory: " + path.toString());
                                e.printStackTrace();
                            }
                        });
                System.out.println("Successfully deleted directory: " + directoryPath.toString());
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static List<String> listProcessChannel() {
        return new ArrayList<>(processMap.keySet());
    }

    public static boolean stopStream(String channelName) {
        Process process = processMap.get(channelName);
        if (process != null && process.isAlive()) {
            process.destroy();
            // Chờ để chắc chắn tiến trình đã dừng
            try {
                process.waitFor(5, TimeUnit.SECONDS); // Đợi tối đa 5 giây
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Phục hồi trạng thái bị ngắt
            }
            // Kiểm tra lại xem tiến trình còn sống không
            if (process.isAlive()) {
                System.out.println("Force stop stream.");
                process.destroyForcibly(); // Dừng mạnh mẽ nếu cần thiết
            }
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

                // Chờ để chắc chắn tiến trình đã dừng
                try {
                    if (!process.waitFor(5, TimeUnit.SECONDS)) { // Đợi tối đa 5 giây
                        System.out.println("Force stop stream.");
                        process.destroyForcibly(); // Dừng mạnh mẽ nếu cần thiết
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Phục hồi trạng thái bị ngắt
                    System.out.println("Error when stop stream: " + e.getMessage());
                }
            }
        }
        processMap.clear(); // Xóa tất cả các kênh từ processMap
    }

    public static void updateChannels(String outputDirectory, String channelName) throws IOException {
        File jsonFile = new File(outputDirectory + "/channels.json");
        if (jsonFile.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            List<ObjectNode> channelList = mapper.readValue(jsonFile, new TypeReference<List<ObjectNode>>() {
            });

            Iterator<ObjectNode> iterator = channelList.iterator();
            while (iterator.hasNext()) {
                ObjectNode channel = iterator.next();
                if (channelName.equals(channel.get("name").asText())) { // Giả sử "id" là trường xác định kênh
                    iterator.remove(); // Xóa kênh
                    System.out.println("===============Deleted channel=============== :" + channelName);
                    break; // Dừng vòng lặp khi đã xóa kênh
                }
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, channelList);
        }
    }
}
