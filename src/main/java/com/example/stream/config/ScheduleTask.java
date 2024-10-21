package com.example.stream.config;

import com.example.stream.service.StreamService;
import com.example.stream.util.FFMpegStreamConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ScheduleTask {
    private final StreamService streamService;

    public ScheduleTask(StreamService streamService) {
        this.streamService = streamService;
    }

    @Scheduled(fixedDelay = 7200000, initialDelay = 0)
    public void getAllChannel() throws IOException {
        System.out.println("Start get list channel");
        FFMpegStreamConverter.stopAllStreams();
        streamService.listChannel(true);
        streamService.startStreamChannels();
        System.out.println("End get list channel");
    }
}
