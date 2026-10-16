package com.wac.autocore.service;


import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(CustomerServiceTest.Config.class)
public class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepo customerRepo;

    @Test
    void createRejectsInvalidPhoneNum() {

        assertThrows(ConstraintViolationException.class,
                () -> customerService.create("Test", "AbsolutelyNotDigits", "test@test.com"));

        verify(customerRepo, never()).save(any());
    }

    @Configuration
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
