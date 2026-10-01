package com.wac.autocore.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;

@SpringBootTest(classes = {AutoConfiguration.class})
@ActiveProfiles("test")
public class HistoricalPricingIntegrationTest {

    @TempDir
    File tempDir;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("spring.datasource.url"));
    }

    @Test
    void test() {}
}
