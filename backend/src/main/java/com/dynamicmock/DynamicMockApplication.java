package com.dynamicmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {"org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration"})
public class DynamicMockApplication {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DynamicMockApplication.class);

    public static void main(String[] args) {
        var context = SpringApplication.run(DynamicMockApplication.class, args);
        var env = context.getEnvironment();
        log.info("--- DIAGNOSTICS ---");
        log.info("SPRING_PROFILES_ACTIVE: {}", env.getProperty("spring.profiles.active"));
        log.info("spring.data.mongodb.uri: {}", env.getProperty("spring.data.mongodb.uri"));
        log.info("spring.data.mongodb.host: {}", env.getProperty("spring.data.mongodb.host"));
        log.info("spring.data.mongodb.port: {}", env.getProperty("spring.data.mongodb.port"));
        log.info("spring.data.mongodb.database: {}", env.getProperty("spring.data.mongodb.database"));
        log.info("-------------------");
    }
}

