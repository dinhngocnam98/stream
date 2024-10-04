package com.example.stream.service;

import ch.qos.logback.core.util.FileUtil;
import com.example.stream.dto.StreamDto;
import com.example.stream.util.ChanelNameExtractor;
import com.example.stream.util.FFMpegStreamConverter;
import com.example.stream.util.ResponseHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class StreamService {
    @Value("${spring.dirHLS}")
    private String outputDirectory;

    private final RestTemplate restTemplate;

    public StreamService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ObjectNode> listChannel(Boolean isGetUrlStream) {
        String response = restTemplate.getForObject("https://stream-proxy.hakinam2701.workers.dev/playlist", String.class);
        String[] listResponse = response.split("\n");
        List<ObjectNode> channelList = new ArrayList<>();
        Set<String> uniqueChannelNames = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();
        int id = 1;
        for (String line : listResponse) {
            String channelName = new ChanelNameExtractor().extractChannelName(line);
            String logo = new ChanelNameExtractor().extractLogo(line);
            ObjectNode channelNode = mapper.createObjectNode();
            if (channelName != null && uniqueChannelNames.add(channelName)) {
                channelNode.put("name", channelName);
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
        return channelList;
    }

    public Boolean startStreamChannel(StreamDto streamDto) {
        List<ObjectNode> listChannel = listChannel(true);
        String extractedStreamURL = "";
        for (ObjectNode currentNode : listChannel) {
            if (currentNode.get("name").asText().equals(streamDto.getChannel())) {
                extractedStreamURL = currentNode.get("streamUrl").asText();
                break;
            }
        }
        if (FFMpegStreamConverter.channelExists(streamDto.getChannel())) {
                return true;
        }
        Process process = FFMpegStreamConverter.startStream(extractedStreamURL, outputDirectory, streamDto.getChannel());
        return true;
    }
}
