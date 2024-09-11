package com.example.stream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;

@Configuration
public class RSocketConfig {

    @Bean
    public RSocketRequester.Builder rSocketRequesterBuilder() {
        return RSocketRequester.builder();
    }}