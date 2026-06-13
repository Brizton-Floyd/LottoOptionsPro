package com.example.lottooptionspro.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    /**
     * Max size of a response body buffered in memory. The lottery dashboard payload is several
     * megabytes of draw-pattern JSON and grows as draw history accumulates, so the default
     * 256 KB WebFlux limit must be raised well above it (a too-small limit makes the client
     * abort mid-response, which surfaced as a confusing NullPointerException downstream).
     * Note this limit applies to the DECODED body size, so it is unaffected by gzip transport
     * compression.
     */
    private static final int MAX_IN_MEMORY_SIZE = 32 * 1024 * 1024; // 32 MB

    @Bean
    @Qualifier("statesServiceWebClient")
    public WebClient statesServiceWebClient(
            @Value("${api.states-service.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }

    @Bean
    @Qualifier("analysisServiceWebClient")
    public WebClient analysisServiceWebClient(
            @Value("${api.analysis-service.base-url}") String baseUrl) {
        // compress(true) makes Reactor Netty advertise "Accept-Encoding: gzip" and transparently
        // decompress responses, so the large dashboard payload travels gzipped on the wire (the
        // API gzips application/json). Decompression happens before buffering, so the
        // MAX_IN_MEMORY_SIZE limit still applies to the full decoded size.
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().compress(true)))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}
