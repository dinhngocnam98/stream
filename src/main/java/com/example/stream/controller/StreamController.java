package com.example.stream.controller;

import com.example.stream.FFMpegStreamConverter;
import com.example.stream.ResponseHandler;
import com.example.stream.dto.StreamDto;
import com.example.stream.service.ChanelNameExtractor;
import com.example.stream.service.TokenService;
import com.example.stream.service.URLExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@RestController
public class StreamController {
    @GetMapping("list")
    private ResponseEntity<ResponseHandler> listChannel() {
        try (BufferedReader br = new BufferedReader(new FileReader("./playlist/channel.txt"))) {
            String line;
            List<String> channelList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String channelName = new ChanelNameExtractor().extractChannelName(line).replace("http", "");
                channelList.add(channelName);
            }
             return new ResponseEntity<>(ResponseHandler.success(channelList), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(ResponseHandler.errorString("Failed to get channel list",400), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/listProcess")
    public ResponseEntity<ResponseHandler> listStreams() {
        return new ResponseEntity<>(ResponseHandler.success(FFMpegStreamConverter.listProcessChannel()), HttpStatus.OK);
    }

    @PostMapping("startStreamChannel")
    public ResponseEntity<ResponseHandler> startSteamChannel(@RequestBody StreamDto streamDto) {
        boolean valid = new TokenService().validateToken(streamDto.getToken());
//        if (!valid) {
//            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
//        }
        try (BufferedReader br = new BufferedReader(new FileReader("./playlist/channel.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String channelName = new ChanelNameExtractor().extractChannelName(line).replace("http", "");
                if (channelName.equals(streamDto.getChannel())) {
                    if (FFMpegStreamConverter.channelExists(streamDto.getChannel())) {
                        return new ResponseEntity<>(ResponseHandler.success(true), HttpStatus.OK);
                    }
                    String extractedStreamURL = URLExtractor.extractURL(line);
                    Process process = FFMpegStreamConverter.startStream(extractedStreamURL, "m3u8", streamDto.getChannel());
                    if (process != null) {
                        return new ResponseEntity<>(ResponseHandler.success(true), HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(ResponseHandler.errorString("Failed to start channel",400), HttpStatus.BAD_REQUEST);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("/stopStreamChannel")
    public ResponseEntity<ResponseHandler> stopStreamChannel(@RequestBody StreamDto streamDto) {
        boolean valid = new TokenService().validateToken(streamDto.getToken());
//        if (!valid) {
//            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
//        }
        boolean stop = FFMpegStreamConverter.stopStream(streamDto.getChannel());
        if (stop) {
            return new ResponseEntity<>(ResponseHandler.success("Stream stopped successfully for channel: " + streamDto.getChannel()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(ResponseHandler.success("No running stream found with the specified channel name."), HttpStatus.OK);
        }
    }

    @GetMapping("token")
    public ResponseEntity<ResponseHandler> generateToken() {
        TokenService tokenService = new TokenService();
        String token = tokenService.generateToken();
        return new ResponseEntity<>(ResponseHandler.success(token), HttpStatus.OK);
    }

    @PostMapping("token")
    public ResponseEntity<ResponseHandler> generateToken(@RequestBody String token) {
        TokenService tokenService = new TokenService();
        boolean valid = tokenService.validateToken(token);
        if (!valid) {
            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(ResponseHandler.success(token), HttpStatus.OK);
    }

    @MessageMapping("startStream")
    public Mono<String> startStream(String channelName) {

        String input = "#EXTINF:-1 tvg-id=\\\"\\\" tvg-name=\\\"|US| Fight Network\\\" tvg-logo=\\\"http://clipart-library.com/images_k/usa-flag-transparent-background/usa-flag-transparent-background-10.png\\\" group-title=\\\"USA SPORTS\\\",|US| Fight Networkhttp://myb24tv.co:80/93125863/69955388/781910";
        // Step 1: Extract the URL
        String extractedStreamURL = URLExtractor.extractURL(input);

        // Step 2: Extract channel name
        String channel = new ChanelNameExtractor().extractChannelName(input);
        String token = new TokenService().generateToken();
        com.example.stream.FFMpegStreamConverter.startStream(extractedStreamURL, "m3u8", channel);
//        String streamUrl = new FFMpegStreamConverter().startFFmpegStream(extractedStreamURL, channel, token);
//        System.out.println("stream url: " + streamUrl);
        String streamUrl = "Success";
        return Mono.justOrEmpty(streamUrl);
    }

    @MessageMapping("validateToken")
    public Mono<Boolean> validateToken(String token, String channelName) {
        return Mono.just(new TokenService().validateToken(token));
    }

}
