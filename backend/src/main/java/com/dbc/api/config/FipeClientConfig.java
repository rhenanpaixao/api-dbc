package com.dbc.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FipeClientConfig {

    @Value("${fipe.api.base-url}")
    private String fipeBaseUrl;

    @Bean
    public RestClient fipeRestClient() {
        return RestClient.builder()
                .baseUrl(fipeBaseUrl)
                .defaultHeader("accept", "application/json")
                .build();
    }
}
