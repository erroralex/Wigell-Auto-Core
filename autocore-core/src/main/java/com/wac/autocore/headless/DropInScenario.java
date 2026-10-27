package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.MechanicService;
import com.wac.autocore.service.PaymentService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderLifecycle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.wac.autocore.headless.ScenarioStep.expectFailure;
import static com.wac.autocore.headless.ScenarioStep.step;

/**
 * <b>DropInScenario</b>
 * <p>Responsibility: Runs a complete drop-in flow through the services without a UI: create, add
 * services and mechanic, confirm, start, complete, invoice and pay. Then two
 * intentional errors: starting a cancelled order and an unknown vehicle.</p>
 * <p>Each step is a <b>Command</b> ({@link ScenarioStep}). This class is the <b>client</b> that
 * builds the list; {@link ScenarioRunner} is the <b>invoker</b> that runs it and translates
 * errors with the same {@link ErrorHandlerChain} as the UI.</p>
 * <p>Depends on {@link WorkOrderLifecycle}, not on {@code WorkOrderService}, so it is the
 * <b>client</b> in the <b>Proxy</b> pattern: the standalone program can hand it an
 * access-checking proxy instead of the real service without any change here.</p>
 * <p>Deliberately without {@code @Transactional}: each service call gets its own transaction, just like
 * when a user clicks in the UI. A shared transaction would also be marked as
 * rollback-only by the intentional errors and roll back the entire flow.</p>
 * <p>Without {@code @Profile}, so the integration test can run the scenario without the headless runner.</p>
 */
@Component
public class DropInScenario {

    private static final int UNKNOWN_VEHICLE_ID = 999_999;
    private static final int SERVICE_COUNT = 2;

    private final WorkOrderLifecycle workOrderLifecycle;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final MechanicService mechanicService;
    private final ServiceItemService serviceItemService;

    public DropInScenario(WorkOrderLifecycle workOrderLifecycle,
                          InvoiceService invoiceService,
                          PaymentService paymentService,
                          VehicleService vehicleService,
                          MechanicService mechanicService,
                          ServiceItemService serviceItemService) {
        this.workOrderLifecycle = workOrderLifecycle;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.vehicleService = vehicleService;
        this.mechanicService = mechanicService;
        this.serviceItemService = serviceItemService;
    }

    /* Responsibility: Runs all steps in order and returns the result of each step that was run */
    public List<StepResult> run() {
        return new ScenarioRunner().run(steps());
    }

    private List<ScenarioStep> steps() {
        return Arrays.asList(
                step("Fetch test data", this::lookUpSeedData),

                step("Create drop-in order", ctx -> ctx.setWorkOrderId(
                        workOrderLifecycle.createDropIn(ctx.getVehicleId(),
                                "Headless demo: check engine light is on",
                                Collections.<Integer>emptyList()).getId())),

                // Drop-in does not allow free lines, so the charge flags are ignored.
                // No start time: the car is taken in directly. Without a start time the order is not checked for clashes,
                step("Add services and mechanic", ctx -> workOrderLifecycle.updateWorkOrder(
                        ctx.getWorkOrderId(), ctx.getServiceItemIds(), ctx.getMechanicId(),
                        LocalDate.now(), null, null, null, Collections.<Integer, Boolean>emptyMap())),

                step("Confirm order", ctx -> workOrderLifecycle.confirmWorkOrder(ctx.getWorkOrderId())),
                step("Start order", ctx -> workOrderLifecycle.startWorkOrder(ctx.getWorkOrderId())),
                step("Complete order", ctx -> workOrderLifecycle.completeWorkOrder(ctx.getWorkOrderId())),

                step("Create invoice with WELCOME10", ctx -> ctx.setInvoiceId(
                        invoiceService.create(ctx.getWorkOrderId(), "WELCOME10").getId())),

                step("Pay by card", ctx -> paymentService.processPayment(ctx.getInvoiceId(), "CARD")),

                step("Create and cancel a second order", this::createAndCancelOrder),

                // Intentional errors: should give the same language keys as in the UI
                expectFailure("Start cancelled order",
                        ctx -> workOrderLifecycle.startWorkOrder(ctx.getCancelledOrderId())),
                expectFailure("Create order for unknown vehicle",
                        ctx -> workOrderLifecycle.createDropIn(UNKNOWN_VEHICLE_ID,
                                "Headless demo: unknown vehicle",
                                Collections.<Integer>emptyList()))
        );
    }

    // Uses the seed data instead of hard-coded ids
    private void lookUpSeedData(ScenarioContext ctx) {
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Mechanic> mechanics = mechanicService.listAll();
        List<ServiceItem> services = serviceItemService.listAll();

        if (vehicles.isEmpty()) {
            throw new IllegalStateException("No test data: the database has no vehicles");
        }
        if (mechanics.isEmpty()) {
            throw new IllegalStateException("No test data: the database has no mechanics");
        }
        if (services.isEmpty()) {
            throw new IllegalStateException("No test data: the database has no services");
        }

        ctx.setVehicleId(vehicles.get(0).getId());
        ctx.setMechanicId(mechanics.get(0).getId());
        ctx.setServiceItemIds(services.stream()
                .limit(SERVICE_COUNT)
                .map(ServiceItem::getId)
                .collect(Collectors.toList()));
    }

    // A regular step that prepares the failure step "Start cancelled order"
    private void createAndCancelOrder(ScenarioContext ctx) {
        int orderId = workOrderLifecycle.createDropIn(ctx.getVehicleId(),
                "Headless demo: order to be cancelled",
                Collections.<Integer>emptyList()).getId();
        workOrderLifecycle.cancelWorkOrder(orderId);
        ctx.setCancelledOrderId(orderId);
    }
}
