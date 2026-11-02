package com.wac.autocore;

import com.wac.autocore.model.WorkOrderEventPublisher;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * <b>AutoCoreConfig</b>
 * <p>Responsibility: Spring Boot configuration. Lives in the root package so Spring finds entities,
 * repositories, services and converters in all subpackages.</p>
 */
@SpringBootApplication
// Turns on method-level security so @PreAuthorize rules on the services are enforced.
// autocore-core only declares the rules (it depends on spring-security-core for the annotations).
// They take effect here, where Spring creates the service proxies. autocore-standalone has no Spring
// and protects the work order operations with WorkOrderLifecycleAccessProxy instead.
@EnableMethodSecurity
public class AutoCoreConfig {

    @Bean
    public WorkOrderEventPublisher workOrderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return event -> applicationEventPublisher.publishEvent(event);
    }

    @Bean
    public SpringAuthorizationEventPublisher springAuthorizationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringAuthorizationEventPublisher(applicationEventPublisher);
    }
}