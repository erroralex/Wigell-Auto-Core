package com.wac.autocore;

import org.springframework.boot.autoconfigure.SpringBootApplication;
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
}