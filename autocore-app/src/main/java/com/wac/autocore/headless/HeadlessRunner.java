package com.wac.autocore.headless;

import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * <b>HeadlessRunner</b>
 * <p>Responsibility: Runs {@link DropInScenario} when the application is started with the {@code headless} profile,
 * and prints each step with {@link ScenarioReportPrinter}, the same report as the standalone
 * program. Errors are translated the same way as in the UI, and the language key is printed
 * so it can be compared with the UI.</p>
 * <p>The language is chosen with {@code --headless.lang=en} (default is Swedish).</p>
 * <p>Before the scenario runs, {@link HeadlessAuthenticator} logs in with the configured account. If the
 * credentials are missing or wrong, the run stops before any operation.</p>
 * <p>If any step did not go as expected, an exception is thrown instead of {@code System.exit}:
 * Spring then closes the context and the process exits with an error code, and tests that load
 * the profile do not kill the test JVM.</p>
 * <p>The report is written with {@code System.out} on purpose. It is the program's output to the user,
 * not diagnostics. The headless profile only logs to file, so through the logger the report would not
 * show in the terminal.</p>
 */
@Component
@Profile("headless")
public class HeadlessRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HeadlessRunner.class);

    private final DropInScenario scenario;
    private final HeadlessAuthenticator authenticator;
    private final String lang;

    public HeadlessRunner(DropInScenario scenario, HeadlessAuthenticator authenticator,
                          @Value("${headless.lang:sv}") String lang) {
        this.scenario = scenario;
        this.authenticator = authenticator;
        this.lang = lang;
    }

    @Override
    public void run(String... args) {
        Translator translator = new ResourceBundleTranslator(resolveLocale());
        ScenarioReportPrinter printer = new ScenarioReportPrinter(System.out, translator);

        System.out.println();
        System.out.println("=== Wigell AutoCore - headless drop-in flow ===");

        // Stops here if the credentials are missing or wrong, so no operation has run.
        authenticate();

        List<StepResult> results;
        try {
            results = scenario.run();
        } finally {
            authenticator.clear();
        }
        printer.print(results);

        if (results.stream().anyMatch(result -> !result.isAsExpected())) {
            throw new IllegalStateException("The headless scenario did not go as expected");
        }
    }

    // The message never contains the password: it is either "credentials are missing" or "Bad credentials".
    private void authenticate() {
        try {
            authenticator.authenticate();
        } catch (AuthenticationException ex) {
            log.warn("Headless run stopped before any operation: {}", ex.getMessage());
            System.err.println("Headless login failed. See logs/autocore-headless.log.");
            throw new IllegalStateException("Headless authentication failed", ex);
        }
    }

    private Locale resolveLocale() {
        return "en".equalsIgnoreCase(lang) ? ResourceBundleTranslator.ENGLISH : ResourceBundleTranslator.SWEDISH;
    }
}
