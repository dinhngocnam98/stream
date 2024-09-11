package com.example.stream.controller;

import com.example.stream.util.FFMpegStreamConverter;
import com.example.stream.util.ResponseHandler;
import com.example.stream.dto.StreamDto;
import com.example.stream.util.ChanelNameExtractor;
import com.example.stream.service.TokenService;
import com.example.stream.util.URLExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RestController
public class StreamController {
    @Value("${spring.dirHLS}")
    private String outputDirectory;
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
        if (!valid) {
            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
        }
        try (BufferedReader br = new BufferedReader(new FileReader("./playlist/channel.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String channelName = new ChanelNameExtractor().extractChannelName(line).replace("http", "");
                if (channelName.equals(streamDto.getChannel())) {
                    if (FFMpegStreamConverter.channelExists(streamDto.getChannel())) {
                        return new ResponseEntity<>(ResponseHandler.success(true), HttpStatus.OK);
                    }
                    String extractedStreamURL = URLExtractor.extractURL(line);
                    Process process = FFMpegStreamConverter.startStream(extractedStreamURL,outputDirectory, streamDto.getChannel());
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
        if (!valid) {
            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
        }
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
}
