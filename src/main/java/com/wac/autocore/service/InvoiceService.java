package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.InvoiceRepo;
import com.wac.autocore.service.discount.*;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * <b>InvoiceService</b>
 * <p>Ansvar: Skapar och hämtar fakturor. Beloppet räknas från arbetsorderns avtalade priser,
 * som frystes vid bokningen, och aldrig från tjänstekatalogens nuvarande priser.</p>
 */
@Validated
@Service
public class InvoiceService {

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

    public Invoice create(@Positive int workOrderId,
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
            throw new ValidationException("error.invoice.alreadyExists", String.valueOf(workOrderId));
        }

        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", vehicleId));
        Customer customer = customerService.findById(vehicle.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", vehicle.getCustomerId()));

        Invoice invoice = new Invoice(workOrderId, LocalDate.now());

        DiscountStrategy strategy = selectStrategy(discountCode);

        // Avtalade priser från arbetsorderns rader, inte katalogens nuvarande priser
        for (WorkOrderItem workOrderItem : workOrder.getItems()) {
            Money amount = workOrderItem.getChargedAmount();

            Money discount = strategy.calculateDiscount(amount);

            if (customer.isVip())
                discount = discount.add(new VipDiscount().calculateDiscount(amount));

            invoice.addLine(workOrderItem.getServiceName(), amount, discount);
        }

        Money amountSum = Money.ZERO;
        Money discountSum = Money.ZERO;
        Money totalSum = Money.ZERO;

        for (InvoiceLine invoiceLine : invoice.getLines()) {
            amountSum = amountSum.add(invoiceLine.getAmount());
            discountSum = discountSum.add(invoiceLine.getDiscount());
            totalSum = totalSum.add(invoiceLine.getTotal());
        }

        invoice.setAmount(amountSum);
        invoice.setDiscount(discountSum);
        invoice.setTotalAmount(totalSum);

        return invoiceRepo.save(invoice);
    }

    public Optional<Invoice> findById(Integer id) {
        return invoiceRepo.findById(id);
    }

    public List<Invoice> findAll() {
        return invoiceRepo.findAll();
    }

    public Invoice update(Invoice invoice) {
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
