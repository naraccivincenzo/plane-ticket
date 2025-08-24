package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(
            new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
        );
        
        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            System.out.println("HTTP Request: " + request.getMethod() + " " + request.getURI());
            request.getHeaders().forEach((key, values) -> 
                values.forEach(value -> System.out.println(key + ": " + value))
            );
            return execution.execute(request, body);
        }));
        
        return restTemplate;
    }
}