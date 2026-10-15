package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorReport;
import java.util.Objects;

/**
 * <b>StepResult</b>
 * <p>Ansvar: Resultatet av ett körda {@link ScenarioStep}: om det lyckades, om det var tänkt att
 * misslyckas, och vid fel den {@link ErrorReport} som felkedjan gav, samma rapport som
 * gränssnittet visar.</p>
 * <p>Skapas bara via {@link #success} och {@link #failure}, så att ett resultat aldrig kan vara
 * både lyckat och ha ett fel. {@link #isAsExpected()} är regeln som både runnern och
 * integrationstestet använder.</p>
 */
public final class StepResult {

    private final String name;
    private final boolean expectedFailure;
    private final boolean succeeded;
    private final ErrorReport error;

    private StepResult(String name, boolean expectedFailure, boolean succeeded, ErrorReport error) {
        this.name = name;
        this.expectedFailure = expectedFailure;
        this.succeeded = succeeded;
        this.error = error;
    }

    static StepResult success(ScenarioStep step) {
        return new StepResult(step.name(), step.expectsFailure(), true, null);
    }

    static StepResult failure(ScenarioStep step, ErrorReport error) {
        Objects.requireNonNull(error, "error");
        return new StepResult(step.name(), step.expectsFailure(), false, error);
    }

    public String getName() {
        return name;
    }

    public boolean isExpectedFailure() {
        return expectedFailure;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public ErrorReport getError() {
        return error;
    }

    // Sant om steget blev som tänkt: ett vanligt steg lyckades, eller ett felsteg misslyckades
    public boolean isAsExpected() {
        if (expectedFailure) {
            return !succeeded && !error.isUnexpected();
        }
        return succeeded;
    }
}
