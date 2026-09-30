package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.InvoiceRepo;
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
    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;

    public InvoiceService(InvoiceRepo invoiceRepo,
                          WorkOrderService workOrderService,
                          BookingService bookingService,
                          VehicleService vehicleService,
                          CustomerService customerService) {
        this.invoiceRepo = invoiceRepo;
        this.workOrderService = workOrderService;
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.customerService = customerService;
    }

    public Invoice create(int workOrderId, String discountCode) {

        WorkOrder workOrder = workOrderService.findById(workOrderId);
        if (workOrder == null) {
            throw new EntityNotFoundException("WorkOrder", workOrderId);
        }

        // Avtalade priser från arbetsorderns rader, inte katalogens nuvarande priser
        double amount = workOrder.getTotalPrice();

        Booking booking = bookingService.findById(workOrder.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking", workOrder.getBookingId()));
        Vehicle vehicle = vehicleService.findById(booking.getVehicleId())
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", booking.getVehicleId()));
        Customer customer = customerService.findById(vehicle.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", vehicle.getCustomerId()));

        double discount = 0;

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
