package com.wac.autocore.headless;

import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * <b>HeadlessRunner</b>
 * <p>Responsibility: Runs {@link DropInScenario} when the application is started with the {@code headless} profile,
 * and prints each step with {@link ScenarioReportPrinter}, the same report as the standalone
 * program. Errors are translated the same way as in the UI, and the language key is printed
 * so it can be compared with the UI.</p>
 * <p>The language is chosen with {@code --headless.lang=en} (default is Swedish).</p>
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

    private final DropInScenario scenario;
    private final String lang;

    private final AuthenticationManager authenticationManager;

    @Value("${AUTOCORE_HEADLESS_USERNAME:}")
    private String username;

    @Value("${AUTOCORE_HEADLESS_PASSWORD:}")
    private String password;
    private static final Logger logger = LoggerFactory.getLogger(HeadlessRunner.class);


    public HeadlessRunner(DropInScenario scenario, @Value("${headless.lang:sv}") String lang, AuthenticationManager authenticationManager) {
        this.scenario = scenario;
        this.lang = lang;

        this.authenticationManager = authenticationManager;
    }

    @Override
    public void run(String... args) {
        Translator translator = new ResourceBundleTranslator(resolveLocale());
        ScenarioReportPrinter printer = new ScenarioReportPrinter(System.out, translator);

        // Validate credentials exist before doing anything
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            logger.warn("Headless run aborted: AUTOCORE_HEADLESS_USERNAME and AUTOCORE_HEADLESS_PASSWORD are missing.");
            throw new IllegalStateException("Missing headless credentials");
        }

        // Authenticate before running the scenario
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (AuthenticationException e) {
            logger.warn("Headless run aborted: Invalid credentials provided.");
            throw new IllegalStateException("Invalid headless credentials", e);
        }

        System.out.println();
        System.out.println("=== Wigell AutoCore - headless drop-in flow ===");

        try {
            // Run the scenario now that we are authenticated
            List<StepResult> results = scenario.run();
            printer.print(results);

            if (results.stream().anyMatch(result -> !result.isAsExpected())) {
                throw new IllegalStateException("The headless scenario did not go as expected");
            }
        } finally {
            // Always clear the context afterwards
            SecurityContextHolder.clearContext();
        }
    }



    private Locale resolveLocale() {
        return "en".equalsIgnoreCase(lang) ? ResourceBundleTranslator.ENGLISH : ResourceBundleTranslator.SWEDISH;
    }
}
