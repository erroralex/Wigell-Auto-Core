package com.wac.autocore.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import ch.qos.logback.classic.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class SecurityEventLoggerTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    private ListAppender<ILoggingEvent> listAppender;

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(SecurityEventLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void shutDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void failedLogin_shouldThrow_andLogDoesNotContainPassword() {
        String username = "failed-login-user";
        String password = "not-log-password";
        assertThrows(BadCredentialsException.class, () ->
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(username, password)
                ));

        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage())
                            .contains("event=login_failure")
                            .contains("username=" + username)
                            .doesNotContain(password);
                });
    }
}
