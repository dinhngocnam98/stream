package com.example.stream.controller;

import com.example.stream.service.StreamService;
import com.example.stream.util.FFMpegStreamConverter;
import com.example.stream.util.ResponseHandler;
import com.example.stream.dto.StreamDto;
import com.example.stream.service.TokenService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.ResourceLoader;
import java.io.*;
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
    private ResponseEntity<ResponseHandler> listChannel() throws IOException {
        List<ObjectNode> channelList = streamService.readChannelsFromFile(false);
        return new ResponseEntity<>(ResponseHandler.success(channelList), HttpStatus.OK);
    }


    @GetMapping("/listProcess")
    public ResponseEntity<ResponseHandler> listStreams() {
        return new ResponseEntity<>(ResponseHandler.success(FFMpegStreamConverter.listProcessChannel()), HttpStatus.OK);
    }

    @PostMapping("/stopStreamChannel")
    public ResponseEntity<ResponseHandler> stopStreamChannel(@RequestBody StreamDto streamDto) {
//        boolean valid = new TokenService().validateToken(streamDto.getToken());
//        if (!valid) {
//            return new ResponseEntity<>(ResponseHandler.errorString("Token invalid or expired",400), HttpStatus.BAD_REQUEST);
//        }
        String stopStreamChannel = streamService.stopStreamChannel(streamDto);
        return new ResponseEntity<>(ResponseHandler.success(stopStreamChannel), HttpStatus.OK);
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
