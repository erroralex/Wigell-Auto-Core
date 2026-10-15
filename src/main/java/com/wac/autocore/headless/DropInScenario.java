package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorHandler;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.wac.autocore.headless.ScenarioStep.expectFailure;
import static com.wac.autocore.headless.ScenarioStep.step;

/**
 * <b>DropInScenario</b>
 * <p>Ansvar: Kör ett komplett drop-in-flöde genom tjänsterna utan gränssnitt: skapa, lägg till
 * tjänster och mekaniker, bekräfta, starta, slutför, fakturera och betala. Därefter två
 * avsiktliga fel: start av en avbruten order och ett okänt fordon.</p>
 * <p>Varje steg är ett <b>Command</b> ({@link ScenarioStep}) som körs på samma sätt: lyckas det
 * blir det {@link StepResult#success}, annars översätts felet av samma
 * {@link ErrorHandlerChain} som gränssnittet använder. Därför får headless samma språknycklar som UI:t.</p>
 * <p>Medvetet utan {@code @Transactional}: varje tjänsteanrop får en egen transaktion, precis som
 * när en användare klickar i gränssnittet. En gemensam transaktion skulle dessutom markeras som
 * rollback-only av de avsiktliga felen och rulla tillbaka hela flödet.</p>
 * <p>Utan {@code @Profile}, så att integrationstestet kan köra scenariot utan headless-runnern.</p>
 */
@Component
public class DropInScenario {

    private static final int UNKNOWN_VEHICLE_ID = 999_999;
    private static final int SERVICE_COUNT = 2;

    private final WorkOrderService workOrderService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final MechanicService mechanicService;
    private final ServiceItemService serviceItemService;
    private final ErrorHandler errorChain = ErrorHandlerChain.createDefault();

    public DropInScenario(WorkOrderService workOrderService,
                          InvoiceService invoiceService,
                          PaymentService paymentService,
                          VehicleService vehicleService,
                          MechanicService mechanicService,
                          ServiceItemService serviceItemService) {
        this.workOrderService = workOrderService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.vehicleService = vehicleService;
        this.mechanicService = mechanicService;
        this.serviceItemService = serviceItemService;
    }

    /* Ansvar: Kör alla steg i ordning och returnerar resultatet av varje steg som kördes */
    public List<StepResult> run() {
        ScenarioContext ctx = new ScenarioContext();
        List<StepResult> results = new ArrayList<>();

        for (ScenarioStep step : steps()) {
            StepResult result;
            try {
                step.execute(ctx);
                result = StepResult.success(step);
            } catch (RuntimeException e) {
                result = StepResult.failure(step, errorChain.resolve(e));
            }
            results.add(result);

            if (!result.isAsExpected() && !step.expectsFailure()) {
                break;
            }
        }
        return results;
    }

    private List<ScenarioStep> steps() {
        return Arrays.asList(
                step("Hämta testdata", this::lookUpSeedData),

                step("Skapa drop-in-order", ctx -> ctx.setWorkOrderId(
                        workOrderService.createDropIn(ctx.getVehicleId(),
                                "Headless-demo: motorlampan lyser",
                                Collections.<Integer>emptyList()).getId())),

                // Drop-in tillåter inga gratisrader, så debiteringsflaggorna ignoreras
                step("Lägg till tjänster och mekaniker", ctx -> workOrderService.updateWorkOrder(
                        ctx.getWorkOrderId(), ctx.getServiceItemIds(), ctx.getMechanicId(),
                        LocalDate.now(), null, null, Collections.<Integer, Boolean>emptyMap())),

                step("Bekräfta order", ctx -> workOrderService.confirmWorkOrder(ctx.getWorkOrderId())),
                step("Starta order", ctx -> workOrderService.startWorkOrder(ctx.getWorkOrderId())),
                step("Slutför order", ctx -> workOrderService.completeWorkOrder(ctx.getWorkOrderId())),

                step("Skapa faktura med WELCOME10", ctx -> ctx.setInvoiceId(
                        invoiceService.create(ctx.getWorkOrderId(), "WELCOME10").getId())),

                step("Betala med kort", ctx -> paymentService.processPayment(ctx.getInvoiceId(), "CARD")),

                step("Skapa och avbryt en andra order", this::createAndCancelOrder),

                // Avsiktliga fel: ska ge samma språknycklar som i gränssnittet
                expectFailure("Starta avbruten order",
                        ctx -> workOrderService.startWorkOrder(ctx.getCancelledOrderId())),
                expectFailure("Skapa order för okänt fordon",
                        ctx -> workOrderService.createDropIn(UNKNOWN_VEHICLE_ID,
                                "Headless-demo: okänt fordon",
                                Collections.<Integer>emptyList()))
        );
    }

    // Använder seed-datan i stället för hårdkodade id:n
    private void lookUpSeedData(ScenarioContext ctx) {
        List<Vehicle> vehicles = vehicleService.findAll();
        List<Mechanic> mechanics = mechanicService.listAll();
        List<ServiceItem> services = serviceItemService.listAll();

        if (vehicles.isEmpty()) {
            throw new IllegalStateException("Ingen testdata: databasen saknar fordon");
        }
        if (mechanics.isEmpty()) {
            throw new IllegalStateException("Ingen testdata: databasen saknar mekaniker");
        }
        if (services.isEmpty()) {
            throw new IllegalStateException("Ingen testdata: databasen saknar tjänster");
        }

        ctx.setVehicleId(vehicles.get(0).getId());
        ctx.setMechanicId(mechanics.get(0).getId());
        ctx.setServiceItemIds(services.stream()
                .limit(SERVICE_COUNT)
                .map(ServiceItem::getId)
                .collect(Collectors.toList()));
    }

    // Ett vanligt steg som förbereder felsteget "Starta avbruten order"
    private void createAndCancelOrder(ScenarioContext ctx) {
        int orderId = workOrderService.createDropIn(ctx.getVehicleId(),
                "Headless-demo: order som avbryts",
                Collections.<Integer>emptyList()).getId();
        workOrderService.cancelWorkOrder(orderId);
        ctx.setCancelledOrderId(orderId);
    }
}
