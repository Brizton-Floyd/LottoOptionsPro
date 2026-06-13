package com.example.lottooptionspro.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebClientConfigTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        StandardEnvironment env = new StandardEnvironment();
        Map<String, Object> props = new HashMap<>();
        props.put("api.states-service.base-url", "http://states.test:8001");
        props.put("api.analysis-service.base-url", "http://analysis.test:8002");
        MutablePropertySources sources = env.getPropertySources();
        sources.addFirst(new MapPropertySource("test-props", props));

        context = new AnnotationConfigApplicationContext();
        context.setEnvironment(env);
        context.register(PropertySourcesPlaceholderConfigurer.class);
        context.register(WebClientConfig.class);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void bothWebClientBeansExist_andAreDistinct() {
        WebClient states = context.getBean("statesServiceWebClient", WebClient.class);
        WebClient analysis = context.getBean("analysisServiceWebClient", WebClient.class);

        assertNotNull(states);
        assertNotNull(analysis);
        assertNotSame(states, analysis);
    }

    @Test
    void statesWebClient_prependsConfiguredBaseUrl() {
        WebClient states = context.getBean("statesServiceWebClient", WebClient.class);
        assertCapturedUrl(states, "/foo", "http://states.test:8001/foo");
    }

    @Test
    void analysisWebClient_prependsConfiguredBaseUrl() {
        WebClient analysis = context.getBean("analysisServiceWebClient", WebClient.class);
        assertCapturedUrl(analysis, "/bar", "http://analysis.test:8002/bar");
    }

    private void assertCapturedUrl(WebClient client, String path, String expectedFullUrl) {
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse emptyResponse = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(emptyResponse));

        client.mutate().exchangeFunction(exchangeFunction).build()
                .get().uri(path)
                .retrieve().bodyToMono(String.class)
                .block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchangeFunction).exchange(captor.capture());
        assertEquals(expectedFullUrl, captor.getValue().url().toString());
    }
}
