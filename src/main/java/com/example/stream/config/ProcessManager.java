package com.example.stream.config;

import com.example.stream.util.FFMpegStreamConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class ProcessManager {

    @PostConstruct
    public void init() {
        // Đăng ký shutdown hook để dừng tất cả các process khi server dừng
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down, stopping all streams...");
            FFMpegStreamConverter.stopAllStreams();
        }));
    }

    @PreDestroy
    public void cleanup() {
        // Cleanup logic if needed
        System.out.println("Cleaning up resources...");
        FFMpegStreamConverter.stopAllStreams();
    }
}