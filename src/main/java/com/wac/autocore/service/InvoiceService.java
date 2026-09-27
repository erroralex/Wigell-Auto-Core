package com.wac.autocore.service;

import com.wac.autocore.model.*;
import com.wac.autocore.repository.InvoiceRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepo invoiceRepo;
    private final WorkOrderService workOrderService;
    private final ServiceItemService serviceItemService;
    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;

    public InvoiceService(InvoiceRepo invoiceRepo, WorkOrderService workOrderService, ServiceItemService serviceItemService, BookingService bookingService, VehicleService vehicleService, CustomerService customerService) {
        this.invoiceRepo = invoiceRepo;
        this.workOrderService = workOrderService;
        this.serviceItemService = serviceItemService;
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
    }

    public Invoice create(int workOrderId, String discountCode) {

        WorkOrder workOrder = this.workOrderService.findById(workOrderId);

        Map<Integer, ServiceItem> itemsById = this.serviceItemService.listAll().stream()
                .collect(Collectors.toMap(ServiceItem::getId, Function.identity()));

        double amount = 0;

        for (Integer serviceItemId : workOrder.getServiceItemIds())
            amount += itemsById.get(serviceItemId).getPrice();

        double discount = 0;

        Booking booking = this.bookingService.findById(workOrder.getBookingId()).orElse(null);
        Vehicle vehicle = this.vehicleService.findById(booking.getVehicleId()).orElse(null);
        Customer customer = this.customerService.findById(vehicle.getCustomerId()).orElse(null);

        if (customer.isVip())
            discount += amount * 0.10;

        if (discountCode != null && !discountCode.trim().isEmpty()) {

            if (discountCode.equalsIgnoreCase("WELCOME10"))
                discount += amount * 0.10;

            else if (discountCode.equalsIgnoreCase("SERVICE200"))
                discount += 200.0;
        }

        if (discount > amount)
            discount = amount;

        Invoice invoice = new Invoice(workOrderId, LocalDate.now(), amount);
        invoice.setDiscount(discount);

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
}