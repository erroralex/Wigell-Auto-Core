package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.i18n.ErrorReportFormatter;
import com.wac.autocore.i18n.LocalizedError;
import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * <b>HeadlessRunner</b>
 * <p>Ansvar: Kör {@link DropInScenario} när applikationen startas med profilen {@code headless},
 * och skriver ut varje steg. Fel översätts av {@link ErrorReportFormatter}, samma som i
 * gränssnittet, och språknyckeln skrivs ut så att den kan jämföras med UI:t.</p>
 * <p>Språk väljs med {@code --headless.lang=en} (standard är svenska).</p>
 * <p>Blev något steg inte som förväntat kastas ett undantag i stället för {@code System.exit}:
 * Spring stänger då kontexten och processen avslutas med felkod, och tester som laddar
 * profilen dödar inte test-JVM:en.</p>
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
        System.out.println("=== Wigell AutoCore - headless drop-in-flöde ===");

        List<StepResult> results = scenario.run();
        for (StepResult result : results) {
            print(result, formatter);
        }

        long asExpected = results.stream().filter(StepResult::isAsExpected).count();
        System.out.println();
        System.out.println(asExpected + " av " + results.size() + " steg blev som förväntat");

        if (asExpected != results.size()) {
            throw new IllegalStateException("Headless-scenariot blev inte som förväntat");
        }
    }

    private Locale resolveLocale() {
        return "en".equalsIgnoreCase(lang) ? ResourceBundleTranslator.ENGLISH : ResourceBundleTranslator.SWEDISH;
    }

    // [OK]/[FEL] i stället för symboler, så att utskriften fungerar även i Windows-konsolen
    private void print(StepResult result, ErrorReportFormatter formatter) {
        String status = result.isAsExpected() ? "[OK] " : "[FEL]";
        String note = result.isExpectedFailure() ? "  (förväntat fel)" : "";
        System.out.println(status + " " + result.getName() + note);

        ErrorReport error = result.getError();
        if (error != null) {
            LocalizedError text = formatter.format(error);
            System.out.println("       [" + error.getMessageKey() + "] "
                    + text.getTitle() + ": " + text.getMessage());

            // Oväntade fel (t.ex. NullPointerException) visar också den tekniska orsaken för felsökning
            if (error.isUnexpected() && error.getCause() != null) {
                Throwable cause = error.getCause();
                System.out.println("       Orsak: " + cause.getClass().getSimpleName()
                        + (cause.getMessage() == null ? "" : " - " + cause.getMessage()));
            }
        }
    }
}
