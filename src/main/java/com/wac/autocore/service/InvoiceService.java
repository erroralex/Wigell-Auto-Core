package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.InvoiceRepo;
import com.wac.autocore.service.discount.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * <b>InvoiceService</b>
 * <p>Ansvar: Skapar och hämtar fakturor. Beloppet räknas från arbetsorderns avtalade priser,
 * som frystes vid bokningen, och aldrig från tjänstekatalogens nuvarande priser.</p>
 */
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

    public Invoice create(int workOrderId, String discountCode) {

        WorkOrder workOrder = workOrderService.findById(workOrderId);
        if (workOrder == null) {
            throw new EntityNotFoundException("WorkOrder", workOrderId);
        }

        Integer vehicleId = workOrder.getVehicleId();
        if (vehicleId == null) {
            throw new EntityNotFoundException("Vehicle", "none on WorkOrder " + workOrderId);
        }

        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", vehicleId));
        Customer customer = customerService.findById(vehicle.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", vehicle.getCustomerId()));

        Invoice invoice = new Invoice(workOrderId, LocalDate.now());

        DiscountStrategy strategy = selectStrategy(discountCode);

        // Avtalade priser från arbetsorderns rader, inte katalogens nuvarande priser
        for (WorkOrderItem workOrderItem : workOrder.getItems()) {
            double amount = workOrderItem.getAgreedPrice();

            double discount = strategy.calculateDiscount(amount);

            if (customer.isVip())
                discount += new VipDiscount().calculateDiscount(amount);

            discount = Math.min(discount, amount);
            double total = amount - discount;

            invoice.addLine(new InvoiceLine(invoice, workOrderItem.getServiceName(), amount, discount, total));
        }

        double amountSum = 0;
        double discountSum = 0;
        double totalSum = 0;

        for (InvoiceLine invoiceLine : invoice.getLines()) {
            amountSum += invoiceLine.getAmount();
            discountSum += invoiceLine.getDiscount();
            totalSum += invoiceLine.getTotal();
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
