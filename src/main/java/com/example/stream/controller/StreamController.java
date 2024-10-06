package com.example.stream.controller;

import ch.qos.logback.core.util.FileUtil;
import com.example.stream.service.StreamService;
import com.example.stream.util.FFMpegStreamConverter;
import com.example.stream.util.ResponseHandler;
import com.example.stream.dto.StreamDto;
import com.example.stream.util.ChanelNameExtractor;
import com.example.stream.service.TokenService;
import com.example.stream.util.URLExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.core.io.ResourceLoader;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@RestController
public class StreamController {

    private final StreamService streamService;
    private final ResourceLoader resourceLoader;

    public StreamController(StreamService streamService,ResourceLoader resourceLoader) {
        this.streamService = streamService;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("list")
    private ResponseEntity<ResponseHandler> listChannel() {
        List<ObjectNode> channelList = streamService.listChannel(false);
        return new ResponseEntity<>(ResponseHandler.success(channelList), HttpStatus.OK);
    }


    @GetMapping("/listProcess")
    public ResponseEntity<ResponseHandler> listStreams() {
        return new ResponseEntity<>(ResponseHandler.success(FFMpegStreamConverter.listProcessChannel()), HttpStatus.OK);
    }

    @PostMapping("startStreamChannel")
    public ResponseEntity<ResponseHandler> startSteamChannel(@RequestBody StreamDto streamDto) {
//        boolean valid = new TokenService().validateToken(streamDto.getToken());
//        if (!valid) {
//            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
//        }
        this.streamService.startStreamChannel(streamDto);
        return new ResponseEntity<>(ResponseHandler.success(true), HttpStatus.OK);
    }

    @GetMapping("/stream")
    public ResponseEntity<Resource> getM3u8File() {
        // Đường dẫn tương đối đến tệp .m3u8
        Path path = Paths.get("src/main/resources/m3u8/SportsGrid.m3u8"); // Điều chỉnh đường dẫn tương đối phù hợp với vị trí của tệp
        Resource resource = resourceLoader.getResource("file:" + path.toAbsolutePath().toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SportsGrid.m3u8")
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(resource);
    }

    @GetMapping("/api/m3u8")
    public ResponseEntity<Resource> getM3u8File(@RequestParam String fileName) {
        try {
            Resource resource = resourceLoader.getResource("file:///root/m3u8/" + fileName);
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/stopStreamChannel")
    public ResponseEntity<ResponseHandler> stopStreamChannel(@RequestBody StreamDto streamDto) {
//        boolean valid = new TokenService().validateToken(streamDto.getToken());
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
            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired", 400), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(ResponseHandler.success(token), HttpStatus.OK);
    }
}
