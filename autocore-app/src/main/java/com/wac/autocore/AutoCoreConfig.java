package com.wac.autocore;

import com.wac.autocore.model.WorkOrderEventPublisher;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * <b>AutoCoreConfig</b>
 * <p>Responsibility: Spring Boot configuration. Lives in the root package so Spring finds entities,
 * repositories, services and converters in all subpackages.</p>
 */
@SpringBootApplication
// Turns on method-level security so @PreAuthorize rules on the services are enforced.
// Placed here in autocore-app so autocore-core stays free of Spring Security.
@EnableMethodSecurity
public class AutoCoreConfig {

    @Bean
    public WorkOrderEventPublisher workOrderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return event -> applicationEventPublisher.publishEvent(event);
    }
}