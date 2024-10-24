package com.example.stream.service;

import com.example.stream.dto.StreamDto;
import com.example.stream.util.ChanelNameExtractor;
import com.example.stream.util.FFMpegStreamConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.time.OffsetDateTime;
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
            FFMpegStreamConverter.startStream(extractedStreamURL, outputDirectory, currentNode.get("id").asText(), currentNode.get("name").asText());
        }
    }

    public String stopStreamChannel(StreamDto streamDto) {
        String channelId = formatChannelName(streamDto.getChannel());
        boolean stop = FFMpegStreamConverter.stopStream(channelId);
        if (stop) {
            return "Stream stopped successfully for channel: " + streamDto.getChannel();
        } else {
            return "No running stream found with the specified channel name.";
        }
    }

    public void listChannel(Boolean isGetUrlStream) throws IOException {
        String response = restTemplate.getForObject("https://stream-proxy.hakinam2701.workers.dev/playlist", String.class);
        List<ObjectNode> allPrograms = fetchAllPrograms();
        String[] listResponse = response.split("\n");
        List<ObjectNode> channelList = new ArrayList<>();
        Set<String> uniqueChannelNames = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z");
        for (String line : listResponse) {
            String groupTitle = new ChanelNameExtractor().extractGroup(line);
            String logo = new ChanelNameExtractor().extractLogo(line);
            String channelName = new ChanelNameExtractor().extractChannelName(line);
            String channelId = new ChanelNameExtractor().extractId(line);
            ObjectNode channelNode = mapper.createObjectNode();
            if (groupTitle != null && uniqueChannelNames.add(channelName)) {
                channelNode.put("name", channelName);
                channelNode.put("group", groupTitle);
                channelNode.put("logo", logo);
                channelNode.put("id", channelId);
                List<ObjectNode> programsForChannel = allPrograms.stream()
                        .filter(program -> program.get("channel").asText().equals(channelId)) // Lọc chương trình theo channelId
                        .sorted((p1, p2) -> {
                            OffsetDateTime start1 = OffsetDateTime.parse(p1.get("start").asText(), formatter);
                            OffsetDateTime start2 = OffsetDateTime.parse(p2.get("start").asText(), formatter);
                            return start1.compareTo(start2); // So sánh thời gian
                        })
                        .toList();
                ArrayNode programsArray = mapper.createArrayNode();
                for (ObjectNode program : programsForChannel) {
                    ObjectNode programNode = mapper.createObjectNode();
                    programNode.put("name", program.get("title").asText());
                    programNode.put("start", program.get("start").asText());
                    programNode.put("stop", program.get("stop").asText());
                    programNode.put("category", program.get("category").asText());
                    programNode.put("channelId", program.get("channel").asText());
                    programsArray.add(programNode); // Thêm programNode vào programsArray
                }
                channelNode.set("programmes", programsArray);
                channelList.add(channelNode);
            }
            if (isGetUrlStream) {
                String streamUrl = new ChanelNameExtractor().extractStreamUrl(line);
                for (ObjectNode currentNode : channelList) {
                    if (streamUrl != null && streamUrl.contains(currentNode.get("id").asText())) {
                        currentNode.put("streamUrl", streamUrl);
                        break;
                    }
                }
            }

        }

        formatChannelList(channelList);
        saveToFile(channelList);
    }

    public List<ObjectNode> fetchAllPrograms() {
        String apiUrl = "https://stream-proxy.hakinam2701.workers.dev/epg"; // URL của API trả về toàn bộ chương trình
        String xmlResponse = restTemplate.getForObject(apiUrl, String.class);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            String xmlString = writer.toString();
            JSONObject jsonObject = XML.toJSONObject(xmlString);
            JSONObject channelData = jsonObject.getJSONObject("tv");
            JSONArray programmes = channelData.getJSONArray("programme");
            List<ObjectNode> programmeList = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < programmes.length(); i++) {
                JSONObject jsonProgramme = programmes.getJSONObject(i);
                JsonNode jsonNode = mapper.readTree(jsonProgramme.toString());
                ObjectNode objectNode = (ObjectNode) jsonNode;
                programmeList.add(objectNode);
            }
            return programmeList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
