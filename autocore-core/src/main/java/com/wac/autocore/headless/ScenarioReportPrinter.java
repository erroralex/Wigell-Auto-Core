package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.i18n.ErrorReportFormatter;
import com.wac.autocore.i18n.LocalizedError;
import com.wac.autocore.i18n.Translator;
import com.wac.autocore.logging.CorrelationIds;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * <b>ScenarioReportPrinter</b>
 * <p>Responsibility: Prints the result of a scenario run, one line per step plus a summary.
 * Shared by the headless runner in autocore-app and the standalone program, so both print
 * exactly the same texts as the GUI.</p>
 * <p>Unexpected errors print the same reference as the UI dialog (the operation's correlation id),
 * followed by the technical cause. The UI hides the cause from end users; the report keeps it
 * because headless and standalone are developer tools run from the terminal.</p>
 */
public final class ScenarioReportPrinter {

    private final PrintStream out;
    private final Translator translator;
    private final ErrorReportFormatter formatter;

    public ScenarioReportPrinter(PrintStream out, Translator translator) {
        this.out = Objects.requireNonNull(out, "out");
        this.translator = Objects.requireNonNull(translator, "translator");
        this.formatter = new ErrorReportFormatter(translator);
    }

    public void print(List<StepResult> results) {
        results.forEach(this::printStep);
        long asExpected = results.stream().filter(StepResult::isAsExpected).count();
        out.println();
        out.println(asExpected + " of " + results.size() + " steps went as expected");
    }

    private void printStep(StepResult result) {
        String status = result.isAsExpected() ? "[OK] " : "[ERR]";
        String note = result.isExpectedFailure() ? "  (expected error)" : "";
        out.println(status + " " + result.getName() + note);

        ErrorReport error = result.getError();
        if (error != null) {
            LocalizedError text = formatter.format(error);
            out.println("       [" + error.getMessageKey() + "] "
                    + text.getTitle() + ": " + text.getMessage());

            // Unexpected errors show the reference like the UI, plus the technical cause since headless is a developer tool
            if (error.isUnexpected() && error.getCause() != null) {
                Throwable cause = error.getCause();
                String reference = CorrelationIds.find(cause);
                out.println("       " + translator.get("error.reference") + " "
                        + (reference == null ? "-" : reference));
                out.println("       Cause: " + cause.getClass().getSimpleName()
                        + (cause.getMessage() == null ? "" : " - " + cause.getMessage()));
            }
        }
    }
}