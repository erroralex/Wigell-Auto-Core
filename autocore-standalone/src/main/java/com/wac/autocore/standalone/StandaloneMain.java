package com.wac.autocore.standalone;

import com.wac.autocore.headless.ScenarioReportPrinter;
import com.wac.autocore.headless.StepResult;
import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import com.wac.autocore.security.Role;

import java.util.List;
import java.util.Locale;

/**
 * <b>StandaloneMain</b>
 * <p>Responsibility: Entry point for running AutoCore without Spring. Reads the role and
 * language, runs the scenario for the role and prints the same report as the headless runner,
 * followed by any invoice.</p>
 * <p>Arguments: {@code --role=ADMIN|WORKSHOP|READ_ONLY} (default ADMIN) and
 * {@code --lang=sv|en} (default sv). Exits with 0 when every step went as expected,
 * 1 when a step did not, and 2 for invalid arguments.</p>
 * <p>Known limits without Spring: Bean Validation and {@code @Transactional} have no effect;
 * the domain rules apply fully, and only work order operations are access checked.</p>
 */
public final class StandaloneMain {

    private static final String ROLE_ARG = "--role=";
    private static final String LANG_ARG = "--lang=";

    private StandaloneMain() {
    }

    public static void main(String[] args) {
        Role role = Role.ADMIN;
        Locale locale = ResourceBundleTranslator.SWEDISH;

        for (String arg : args) {
            if (arg.startsWith(ROLE_ARG)) {
                try {
                    role = Role.valueOf(arg.substring(ROLE_ARG.length()).toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    exitWithUsage("Unknown role: " + arg.substring(ROLE_ARG.length()));
                    return;
                }
            } else if (arg.startsWith(LANG_ARG)) {
                String lang = arg.substring(LANG_ARG.length());
                if ("en".equalsIgnoreCase(lang)) {
                    locale = ResourceBundleTranslator.ENGLISH;
                } else if ("sv".equalsIgnoreCase(lang)) {
                    locale = ResourceBundleTranslator.SWEDISH;
                } else {
                    exitWithUsage("Unknown language: " + lang);
                    return;
                }
            } else {
                exitWithUsage("Unknown argument: " + arg);
                return;
            }
        }

        Translator translator = new ResourceBundleTranslator(locale);
        StandaloneAutoCore autoCore = new StandaloneAutoCore(role);

        System.out.println();
        System.out.println("=== Wigell AutoCore - standalone (role: " + role + ") ===");

        List<StepResult> results = autoCore.runScenario();
        new ScenarioReportPrinter(System.out, translator).print(results);

        StandaloneInvoicePrinter invoicePrinter = new StandaloneInvoicePrinter(System.out, translator);
        autoCore.invoices().forEach(invoicePrinter::print);

        boolean allAsExpected = results.stream().allMatch(StepResult::isAsExpected);
        System.exit(allAsExpected ? 0 : 1);
    }

    // Invalid arguments: say what was wrong, show how to call the program, exit with 2.
    private static void exitWithUsage(String problem) {
        System.err.println(problem);
        System.err.println("Usage: StandaloneMain [--role=ADMIN|WORKSHOP|READ_ONLY] [--lang=sv|en]");
        System.exit(2);
    }
}
