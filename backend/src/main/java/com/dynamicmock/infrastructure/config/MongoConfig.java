package com.dynamicmock.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration
 * Using Spring Boot auto-configuration - no need to extend AbstractMongoClientConfiguration
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.dynamicmock.infrastructure.persistence.mongodb.repository")
public class MongoConfig {
    // Spring Boot will auto-configure MongoDB using application properties
}

