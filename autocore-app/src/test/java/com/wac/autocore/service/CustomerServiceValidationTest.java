package com.wac.autocore.service;

import com.wac.autocore.repository.CustomerRepo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;


import jakarta.validation.ConstraintViolationException;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@SpringJUnitConfig(CustomerServiceValidationTest.Config.class)
@ActiveProfiles("customer-service-validation")
public class CustomerServiceValidationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepo customerRepo;

    @Test
    void createRejectsBlankNameBeforeSaving() {
        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> customerService.create("  ", "070-1234567", "")
        );

        assertTrue(exception.getConstraintViolations().stream()
                .anyMatch(violation ->
                        violation.getMessageTemplate()
                                .equals("error.customer.nameRequired")));

        Mockito.verify(customerRepo, never()).save(any());
    }

    @TestConfiguration
    @Profile("customer-service-validation")
    static class Config {
        @Bean
        CustomerRepo customerRepo() {
            return mock(CustomerRepo.class);
        }

        @Bean
        CustomerService customerService(CustomerRepo customerRepo) {
            return new CustomerService(customerRepo);
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }

        @Bean
        static MethodValidationPostProcessor methodValidationPostProcessor() {
            return new MethodValidationPostProcessor();
        }
    }
}
