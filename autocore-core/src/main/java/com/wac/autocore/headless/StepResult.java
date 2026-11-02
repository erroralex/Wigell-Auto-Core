package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorReport;
import java.util.Objects;

/**
 * <b>StepResult</b>
 * <p>Responsibility: The result of an executed {@link ScenarioStep}: whether it succeeded, whether it was meant to
 * fail, and on failure the {@link ErrorReport} that the error chain produced, the same report that
 * the UI shows.</p>
 * <p>Created only via {@link #success} and {@link #failure}, so a result can never be
 * both successful and have an error. {@link #isAsExpected()} is the rule that both the runner and
 * the integration test use.</p>
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

    // True if the step went as intended: a regular step succeeded, or a failure step failed
    public boolean isAsExpected() {
        if (expectedFailure) {
            return !succeeded && !error.isUnexpected();
        }
        return succeeded;
    }
}
