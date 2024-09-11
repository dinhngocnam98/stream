package com.example.stream;

import com.example.stream.service.URLExtractor;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

public class TestMain {

    public static void main(String[] args) {
        // Create RSocket client connection to the server at localhost on port 7000
        RSocketRequester requester = RSocketRequester.builder()
                .connectTcp("localhost", 7000) // Change to your RSocket server address and port
                .block();

        Mono<String> startStreamResponse = requester.route("startStream")
                .data("Fight Network")
                .retrieveMono(String.class);

        startStreamResponse.subscribe(
                response -> System.out.println("Received response: " + response),
                error -> System.err.println("Error: " + error)
        );
    }
}
