package com.example.stream.service;

import com.example.stream.dto.StreamDto;
import com.example.stream.util.ChanelNameExtractor;
import com.example.stream.util.FFMpegStreamConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class StreamService {
    @Value("${spring.dirHLS}")
    private String outputDirectory;

    private final RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    public StreamService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ObjectNode> readChannelsFromFile(Boolean isGetStreamUrl) throws IOException {
        File jsonFile = new File(outputDirectory + "/channels.json");

        // Kiểm tra xem file có tồn tại không
        if (!jsonFile.exists()) {
            System.out.println("File does not exist: " + outputDirectory + "channels.json");
            return new ArrayList<>(); // Trả về danh sách rỗng nếu file không tồn tại
        }

        // Tạo ObjectMapper để parse JSON
        ObjectMapper mapper = new ObjectMapper();

        // Đọc dữ liệu từ file JSON và chuyển thành danh sách các ObjectNode
        List<ObjectNode> channelList = mapper.readValue(jsonFile, new TypeReference<List<ObjectNode>>() {
        });

        if (!isGetStreamUrl) {
            for (ObjectNode channel : channelList) {
                channel.remove("streamUrl");
            }
        }

        return channelList;
    }

    public void startStreamChannels() throws IOException {
        List<ObjectNode> listChannel = readChannelsFromFile(true);
        String extractedStreamURL = "";
        for (ObjectNode currentNode : listChannel) {
            extractedStreamURL = currentNode.get("streamUrl").asText();
            String channelBase64 = formatChannelName(currentNode.get("name").asText());
            FFMpegStreamConverter.startStream(extractedStreamURL, outputDirectory, channelBase64, currentNode.get("name").asText());
        }
    }



    public String startStreamChannel(StreamDto streamDto) throws IOException {
        List<ObjectNode> listChannel = readChannelsFromFile(true);
        String extractedStreamURL = "";
        String channelBase64 = formatChannelName(streamDto.getChannel());
        for (ObjectNode currentNode : listChannel) {
            if (currentNode.get("name").asText().equals(streamDto.getChannel())) {
                extractedStreamURL = currentNode.get("streamUrl").asText();
                break;
            }
        }
        return channelBase64;
    }

    public String stopStreamChannel(StreamDto streamDto) {
        String channelBase64 = formatChannelName(streamDto.getChannel());
        boolean stop = FFMpegStreamConverter.stopStream(channelBase64);
        if (stop) {
            return "Stream stopped successfully for channel: " + streamDto.getChannel();
        } else {
            return "No running stream found with the specified channel name.";
        }
    }

    public void listChannel(Boolean isGetUrlStream) throws IOException {
        String response = restTemplate.getForObject("https://stream-proxy.hakinam2701.workers.dev/playlist", String.class);
        String[] listResponse = response.split("\n");
        List<ObjectNode> channelList = new ArrayList<>();
        Set<String> uniqueChannelNames = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();
        int id = 1;
        for (String line : listResponse) {
            String groupTitle = new ChanelNameExtractor().extractGroup(line);
            String logo = new ChanelNameExtractor().extractLogo(line);
            String channelName = new ChanelNameExtractor().extractChannelName(line);
            ObjectNode channelNode = mapper.createObjectNode();
            if (groupTitle != null && uniqueChannelNames.add(channelName)) {
                channelNode.put("name", channelName);
                channelNode.put("group", groupTitle);
                channelNode.put("logo", logo);
                channelNode.put("id", id++);
                channelList.add(channelNode);
            }
            if (isGetUrlStream) {
                String streamUrl = new ChanelNameExtractor().extractStreamUrl(line);
                for (ObjectNode currentNode : channelList) {
                    if (currentNode.get("id").asInt() == (id - 1)) {
                        currentNode.put("streamUrl", streamUrl);
                        break;
                    }
                }
            }

        }

        formatChannelList(channelList);
        saveToFile(channelList);
    }

    private void saveToFile(List<ObjectNode> channelList) throws IOException {
        // Tạo đường dẫn đến file JSON trong thư mục resources
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Directory created: " + outputDirectory);
            } else {
                System.out.println("Failed to create directory: " + outputDirectory);
            }
        }

        // Tạo file JSON trong thư mục
        File jsonFile = new File(directory, "channels.json");
        if (!jsonFile.exists()) {
            boolean isFileCreated = jsonFile.createNewFile();
            if (isFileCreated) {
                System.out.println("File created: " + jsonFile.getPath());
            } else {
                System.out.println("File already exists: " + jsonFile.getPath());
            }
        }


        // Ghi dữ liệu vào file
        try (FileOutputStream fos = new FileOutputStream(jsonFile)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fos, channelList);
        }
    }

    public String formatChannelName(String channelName) {
        // Mã hóa bằng Base64
        return Base64.getEncoder().encodeToString(channelName.getBytes());
    }

    public void formatChannelList(List<ObjectNode> channelList) {
        for (ObjectNode channelNode : channelList) {
            if (channelNode.get("streamUrl") != null && channelNode.get("streamUrl").asText().contains("thetvapp")) {
                String[] channelNameSplit = channelNode.get("name").asText().split("-");
                channelNode.put("name", channelNameSplit[1].trim());

                String date = channelNameSplit[3].trim().replace("(", "").replace(")", "");
                String dateTime = channelNameSplit[2].trim();
                String combinedDateTime = date.trim() + " " + dateTime.trim();

                Locale locale = new Locale("en");
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a z", locale);
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(combinedDateTime, inputFormatter);
                ZonedDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
                channelNode.put("time", utcDateTime.toString().replace("[UTC]", ""));
            }
        }
    }
}
