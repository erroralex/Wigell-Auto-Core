package com.wac.autocore.standalone;

import com.wac.autocore.headless.DropInScenario;
import com.wac.autocore.headless.StepResult;
import com.wac.autocore.security.AccessPolicy;
import com.wac.autocore.security.Permission;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.MechanicService;
import com.wac.autocore.service.PaymentService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderLifecycle;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.standalone.repository.InMemoryBookingRepository;
import com.wac.autocore.standalone.repository.InMemoryCustomerRepo;
import com.wac.autocore.standalone.repository.InMemoryInvoiceRepo;
import com.wac.autocore.standalone.repository.InMemoryMechanicRepository;
import com.wac.autocore.standalone.repository.InMemoryPaymentRepo;
import com.wac.autocore.standalone.repository.InMemoryServiceItemRepository;
import com.wac.autocore.standalone.repository.InMemoryVehicleRepo;
import com.wac.autocore.standalone.repository.InMemoryWorkOrderRepository;
import com.wac.autocore.standalone.security.WorkOrderLifecycleAccessProxy;

import java.util.List;
import java.util.Objects;

/**
 * <b>StandaloneAutoCore</b>
 * <p>Responsibility: Builds AutoCore without Spring. Creates the in-memory repositories and
 * the services with {@code new}, in dependency order, so the same service classes run here
 * as in autocore-app.</p>
 * <p>This is the <b>composition root</b> (dependency injection): the only place the object
 * graph is wired.</p>
 * <p>Work order operations from the scenario go through a {@link WorkOrderLifecycleAccessProxy},
 * so the role given at start decides what the run may do.</p>
 */
public final class StandaloneAutoCore {

    private final Role role;

    private final WorkOrderLifecycle workOrderLifecycle;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final MechanicService mechanicService;
    private final ServiceItemService serviceItemService;

    public StandaloneAutoCore(Role role) {
        this.role = Objects.requireNonNull(role, "role");

        InMemoryWorkOrderRepository workOrderRepo = new InMemoryWorkOrderRepository();
        InMemoryBookingRepository bookingRepo = new InMemoryBookingRepository(workOrderRepo);
        InMemoryMechanicRepository mechanicRepo = new InMemoryMechanicRepository();
        InMemoryServiceItemRepository serviceItemRepo = new InMemoryServiceItemRepository();
        InMemoryVehicleRepo vehicleRepo = new InMemoryVehicleRepo();
        InMemoryCustomerRepo customerRepo = new InMemoryCustomerRepo();
        InMemoryInvoiceRepo invoiceRepo = new InMemoryInvoiceRepo();
        InMemoryPaymentRepo paymentRepo = new InMemoryPaymentRepo();

        // Seed data, saved straight to the repositories before any service runs.
        StandaloneSeedData.seed(customerRepo, vehicleRepo, mechanicRepo, serviceItemRepo);

        CustomerService customerService = new CustomerService(customerRepo);
        this.vehicleService = new VehicleService(vehicleRepo, customerRepo);
        BookingService bookingService =
                new BookingService(bookingRepo, serviceItemRepo, mechanicRepo, vehicleRepo);
        WorkOrderService workOrderService = new WorkOrderService(workOrderRepo, bookingRepo,
                mechanicRepo, vehicleRepo, serviceItemRepo, bookingService);
        this.invoiceService =
                new InvoiceService(invoiceRepo, workOrderService, vehicleService, customerService);
        this.paymentService = new PaymentService(paymentRepo, invoiceService);
        this.mechanicService = new MechanicService(mechanicRepo, bookingRepo, workOrderRepo);
        this.serviceItemService = new ServiceItemService(serviceItemRepo);

        this.workOrderLifecycle = new WorkOrderLifecycleAccessProxy(workOrderService, role);
    }

    private DropInScenario dropInScenario() {
        return new DropInScenario(workOrderLifecycle, invoiceService, paymentService,
                vehicleService, mechanicService, serviceItemService);
    }

    // Runs the drop-in flow, or the read-only flow if the role may not change work orders
    public List<StepResult> runScenario() {
        if (AccessPolicy.can(role, Permission.WORK_ORDER_WRITE)) {
            return dropInScenario().run();
        }
        return new ReadOnlyScenario(vehicleService, workOrderLifecycle).run();
    }
}
