package com.dynamicmock.infrastructure.config;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration.
 * Explicitly enables MongoDB repositories for the persistence package 
 * to disambiguate from Redis (avoids "strict repository configuration mode").
 * Creates a dedicated MongoClient bean to ensure the connection URI 
 * is used definitively, bypassing any auto-configuration confusion.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.dynamicmock.infrastructure.persistence.mongodb.repository")
public class MongoConfig {

    @Bean
    public MongoClient mongoClient(
            @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/dynamicmock}") String uri) {
        return MongoClients.create(uri);
    }
}
