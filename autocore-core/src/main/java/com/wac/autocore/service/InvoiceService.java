package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.logging.LogKeys;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.InvoiceRepo;
import com.wac.autocore.service.discount.DiscountStrategy;
import com.wac.autocore.service.discount.NoDiscount;
import com.wac.autocore.service.discount.Service200Discount;
import com.wac.autocore.service.discount.VipDiscount;
import com.wac.autocore.service.discount.Welcome10Discount;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * <b>InvoiceService</b>
 * <p>Responsibility: Creates and fetches invoices. The amount is calculated from the work order's agreed prices,
 * which were frozen at booking time, and never from the service catalog's current prices.</p>
 */
@Validated
@Service
@Transactional
@PreAuthorize("@access.can('DATA_READ')") // Ensures that only users with 'DATA_READ' permission can access the service methods.
public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepo invoiceRepo;
    private final WorkOrderService workOrderService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;

    public InvoiceService(InvoiceRepo invoiceRepo,
                          WorkOrderService workOrderService,
                          VehicleService vehicleService,
                          CustomerService customerService) {
        this.invoiceRepo = invoiceRepo;
        this.workOrderService = workOrderService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
    }

    @PreAuthorize("@access.can('INVOICE_CREATE')") // Ensures that only users with 'INVOICE_CREATE' permission can create invoices.
    public Invoice create(@Positive @NotNull(message="error.invoice.noWorkOrder") Integer workOrderId,
                          @Pattern(
                                  regexp = "^(?:[A-Z]+\\d+)?$",
                                  message = "error.discountCode.invalidFormat"
                          ) String discountCode) {

        WorkOrder workOrder = workOrderService.findById(workOrderId)
                .orElseThrow(() -> new EntityNotFoundException("WorkOrder", workOrderId));

        Integer vehicleId = workOrder.getVehicleId();
        if (vehicleId == null) {
            throw new EntityNotFoundException("Vehicle", "none on WorkOrder " + workOrderId);
        }

        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new ValidationException("error.invoice.workOrderNotCompleted");
        }

        if (invoiceRepo.existsByWorkOrderId(workOrderId)) {
            throw new ValidationException("error.invoice.alreadyInvoiced", String.valueOf(workOrderId));
        }

        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", vehicleId));
        Customer customer = customerService.findById(vehicle.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", vehicle.getCustomerId()));

        Invoice invoice = new Invoice(workOrderId, LocalDate.now());

        DiscountStrategy strategy = selectStrategy(discountCode);

        // Agreed prices from the work order's lines, not the catalog's current prices
        for (WorkOrderItem workOrderItem : workOrder.getItems()) {
            Money amount = workOrderItem.getChargedAmount();

            Money discount = strategy.calculateDiscount(amount);

            if (customer.isVip())
                discount = discount.add(new VipDiscount().calculateDiscount(amount));

            invoice.addLine(workOrderItem.getServiceName(), amount, discount);
        }

        Invoice saved = invoiceRepo.save(invoice);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(saved.getWorkOrderId()))) {
            log.info("Created invoice {} with {} lines", saved.getId(), saved.getLines().size());
        }
        return saved;
    }

    public Optional<Invoice> findById(Integer id) {
        return invoiceRepo.findById(id);
    }

    public List<Invoice> findAll() {
        return invoiceRepo.findAll();
    }

    // Replaces the previous invoice update-method to mark an invoice as paid with authentication and authorization checks as methods above.
    @PreAuthorize("@access.can('PAYMENT_REGISTER')")
    public Invoice markPaid(@Positive @NotNull(message="error.payment.noInvoiceId") Integer invoiceId) {
        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", invoiceId));
        invoice.markPaid();
        return invoiceRepo.save(invoice);
    }

    private DiscountStrategy selectStrategy(String discountCode) {

        if ("WELCOME10".equals(discountCode))
            return new Welcome10Discount();

        if ("SERVICE200".equals(discountCode))
            return new Service200Discount();

        return new NoDiscount();
    }
}
