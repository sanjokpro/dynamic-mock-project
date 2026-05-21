package com.dynamicmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {"org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration"})
public class DynamicMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicMockApplication.class, args);
    }
}

