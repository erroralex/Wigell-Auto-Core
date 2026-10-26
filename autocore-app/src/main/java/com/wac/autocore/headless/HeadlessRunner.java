package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.i18n.ErrorReportFormatter;
import com.wac.autocore.i18n.LocalizedError;
import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import com.wac.autocore.logging.CorrelationIds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * <b>HeadlessRunner</b>
 * <p>Responsibility: Runs {@link DropInScenario} when the application is started with the {@code headless} profile,
 * and prints each step. Errors are translated by {@link ErrorReportFormatter}, the same as in
 * the UI, and the language key is printed so it can be compared with the UI.</p>
 * <p>The language is chosen with {@code --headless.lang=en} (default is Swedish).</p>
 * <p>If any step did not go as expected, an exception is thrown instead of {@code System.exit}:
 * Spring then closes the context and the process exits with an error code, and tests that load
 * the profile do not kill the test JVM.</p>
 * <p>The report is written with {@code System.out} on purpose. It is the program's output to the user,
 * not diagnostics. The headless profile only logs to file, so through the logger the report would not
 * show in the terminal.</p>
 * <p>Unexpected errors print the same reference as the UI dialog (the operation's correlation id),
 * followed by the technical cause. The UI hides the cause from end users; headless keeps it because
 * it is a developer tool run from the terminal.</p>
 */
@Component
@Profile("headless")
public class HeadlessRunner implements CommandLineRunner {

    private final DropInScenario scenario;
    private final String lang;

    public HeadlessRunner(DropInScenario scenario, @Value("${headless.lang:sv}") String lang) {
        this.scenario = scenario;
        this.lang = lang;
    }

    @Override
    public void run(String... args) {
        Translator translator = new ResourceBundleTranslator(resolveLocale());
        ErrorReportFormatter formatter = new ErrorReportFormatter(translator);

        System.out.println();
        System.out.println("=== Wigell AutoCore - headless drop-in flow ===");

        // Run the scenario as a system ADMIN, so it can do everything without logging in.
        List<StepResult> results = runAsSystem(scenario::run);
        for (StepResult result : results) {
            print(result, formatter, translator);
        }

        long asExpected = results.stream().filter(StepResult::isAsExpected).count();
        System.out.println();
        System.out.println(asExpected + " of " + results.size() + " steps went as expected");

        if (asExpected != results.size()) {
            throw new IllegalStateException("The headless scenario did not go as expected");
        }
    }

    // Runs the scenario as a system ADMIN on this thread only. The context is always cleared afterwards.
    private List<StepResult> runAsSystem(Supplier<List<StepResult>> action) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("headless", null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Locale resolveLocale() {
        return "en".equalsIgnoreCase(lang) ? ResourceBundleTranslator.ENGLISH : ResourceBundleTranslator.SWEDISH;
    }

    // [OK]/[ERR] instead of symbols, so the output also works in the Windows console
    private void print(StepResult result, ErrorReportFormatter formatter, Translator translator) {
        String status = result.isAsExpected() ? "[OK] " : "[ERR]";
        String note = result.isExpectedFailure() ? "  (expected error)" : "";
        System.out.println(status + " " + result.getName() + note);

        ErrorReport error = result.getError();
        if (error != null) {
            LocalizedError text = formatter.format(error);
            System.out.println("       [" + error.getMessageKey() + "] "
                    + text.getTitle() + ": " + text.getMessage());

            // Unexpected errors show the reference like the UI, plus the technical cause since headless is a developer tool
            if (error.isUnexpected() && error.getCause() != null) {
                Throwable cause = error.getCause();
                String reference = CorrelationIds.find(cause);
                System.out.println("       " + translator.get("error.reference") + " "
                        + (reference == null ? "-" : reference));
                System.out.println("       Cause: " + cause.getClass().getSimpleName()
                        + (cause.getMessage() == null ? "" : " - " + cause.getMessage()));
            }
        }
    }
}
