package com.wac.autocore.standalone;

import com.wac.autocore.headless.ScenarioContext;
import com.wac.autocore.headless.ScenarioRunner;
import com.wac.autocore.headless.ScenarioStep;
import com.wac.autocore.headless.StepResult;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderLifecycle;

import java.util.List;
import java.util.Objects;

import static com.wac.autocore.headless.ScenarioStep.expectFailure;
import static com.wac.autocore.headless.ScenarioStep.step;

/**
 * <b>ReadOnlyScenario</b>
 * <p>Responsibility: Shows that a role without write permission can read but not change
 * work orders: fetching data succeeds and creating a drop-in order is denied.</p>
 * <p>A <b>client</b> in the <b>Command</b> pattern, like {@code DropInScenario}: it builds the
 * steps and lets {@link ScenarioRunner} run them. The denial is an expected failure, so a
 * read-only run that is stopped counts as going as expected.</p>
 */
final class ReadOnlyScenario {

    private final VehicleService vehicleService;
    private final WorkOrderLifecycle workOrderLifecycle;

    ReadOnlyScenario(VehicleService vehicleService, WorkOrderLifecycle workOrderLifecycle) {
        this.vehicleService = Objects.requireNonNull(vehicleService, "vehicleService");
        this.workOrderLifecycle = Objects.requireNonNull(workOrderLifecycle, "workOrderLifecycle");
    }

    List<StepResult> run() {
        return new ScenarioRunner().run(steps());
    }

    private List<ScenarioStep> steps() {
        return List.of(
                step("Fetch test data", this::lookUpVehicle),

                // Expected to be denied by the access proxy: the role lacks WORK_ORDER_WRITE.
                expectFailure("Create drop-in order",
                        ctx -> workOrderLifecycle.createDropIn(ctx.getVehicleId(),
                                "Standalone demo: read-only attempt", List.of()))
        );
    }

    // Reading is allowed for every role, so this step succeeds even for READ_ONLY.
    private void lookUpVehicle(ScenarioContext ctx) {
        List<Vehicle> vehicles = vehicleService.findAll();
        if (vehicles.isEmpty()) {
            throw new IllegalStateException("No test data: there are no vehicles");
        }
        ctx.setVehicleId(vehicles.get(0).getId());
    }
}
