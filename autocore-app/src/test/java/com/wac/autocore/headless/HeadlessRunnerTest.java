package com.wac.autocore.headless;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * <b>HeadlessRunnerTest</b>
 * <p>Responsibility: Proves that headless mode without credentials stops before any operation. The scenario is a
 * mock, so {@code verifyNoInteractions} shows that it never started. The check does not depend on the database:
 * the scenario's first step is a read, which would also fail without a login.</p>
 */
class HeadlessRunnerTest {

    private final DropInScenario scenario = mock(DropInScenario.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

    private Logger runnerLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void attachLogAppender() {
        runnerLogger = (Logger) LoggerFactory.getLogger(HeadlessRunner.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        runnerLogger.addAppender(listAppender);
    }

    @AfterEach
    void detachLogAppender() {
        runnerLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    @DisplayName("Without credentials the runner logs a WARN and stops before the scenario and before any login")
    void stopsBeforeAnyOperationWhenCredentialsAreMissing() {
        // Empty properties: no username and no password
        HeadlessRunner runner = new HeadlessRunner(scenario,
                new HeadlessAuthenticator(authenticationManager, new HeadlessProperties()), "en");

        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        IllegalStateException thrown;
        try {
            thrown = assertThrows(IllegalStateException.class, () -> runner.run());
        } finally {
            System.setErr(originalErr);
        }

        // The exception is what gives the non-zero exit code, and its cause says why
        assertInstanceOf(AuthenticationCredentialsNotFoundException.class, thrown.getCause());
        verifyNoInteractions(scenario, authenticationManager);

        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("stopped before any operation");
                });
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("Headless login failed");
    }
}
