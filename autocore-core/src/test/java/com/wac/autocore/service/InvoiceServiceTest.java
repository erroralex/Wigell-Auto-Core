package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.InvoiceRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <b>InvoiceServiceTest</b>
 * <p>Responsibility: Locks down how the discount is rounded per line and that the invoice totals are the sum of the lines.</p>
 */
class InvoiceServiceTest {

    private InvoiceRepo invoiceRepo;
    private WorkOrderService workOrderService;
    private Customer customer;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceRepo = mock(InvoiceRepo.class);
        workOrderService = mock(WorkOrderService.class);
        VehicleService vehicleService = mock(VehicleService.class);
        CustomerService customerService = mock(CustomerService.class);
        customer = mock(Customer.class);
        Vehicle vehicle = mock(Vehicle.class);

        when(vehicle.getCustomerId()).thenReturn(7);
        when(vehicleService.findById(5)).thenReturn(Optional.of(vehicle));
        when(customerService.findById(7)).thenReturn(Optional.of(customer));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        invoiceService = new InvoiceService(invoiceRepo, workOrderService, vehicleService, customerService);
    }

    @Test
    void discountIsRoundedPerLine_AndInvoiceTotalsAreTheSumOfTheLines() {
        givenWorkOrderWithPrices("1.05", "1.05", "99.95");

        Invoice invoice = invoiceService.create(1, "WELCOME10");

        assertMoney(Money.of("0.11"), invoice.getLines().get(0).getDiscount());
        assertMoney(Money.of("10.00"), invoice.getLines().get(2).getDiscount());
        assertMoney(Money.of("102.05"), invoice.getAmount());
        assertMoney(Money.of("10.22"), invoice.getDiscount());
        assertMoney(Money.of("91.83"), invoice.getTotalAmount());
        assertMoney(invoice.getAmount().subtract(invoice.getDiscount()), invoice.getTotalAmount());
    }

    @Test
    void vipAndDiscountCodeAreRoundedSeparately() {
        when(customer.isVip()).thenReturn(true);
        givenWorkOrderWithPrices("0.05");

        Invoice invoice = invoiceService.create(1, "WELCOME10");

        // Today's rule: 0,01 + 0,01. A single rounded 20 % would have given 0,01.
        assertMoney(Money.of("0.02"), invoice.getLines().get(0).getDiscount());
    }

    @Test
    void lineNeverGetsANegativeTotal() {
        givenWorkOrderWithPrices("150", "300");

        Invoice invoice = invoiceService.create(1, "SERVICE200");

        assertMoney(Money.of("150"), invoice.getLines().get(0).getDiscount());
        assertMoney(Money.ZERO, invoice.getLines().get(0).getTotal());
        assertMoney(Money.of("200"), invoice.getLines().get(1).getDiscount());
        assertEquals(2, invoice.getLines().size());
    }

    private void givenWorkOrderWithPrices(String... prices) {
        WorkOrderItem[] items = new WorkOrderItem[prices.length];
        for (int i = 0; i < prices.length; i++) {
            ServiceItem service = mock(ServiceItem.class);
            when(service.getId()).thenReturn(i + 1);
            when(service.getName()).thenReturn("Service " + (i + 1));
            when(service.getPrice()).thenReturn(Money.of(prices[i]));
            when(service.getEstimatedMinutes()).thenReturn(30);
            items[i] = WorkOrderItem.snapshotOf(service);
        }

        WorkOrder workOrder = mock(WorkOrder.class);
        when(workOrder.getStatus()).thenReturn(WorkOrderStatus.COMPLETED);
        when(workOrder.getVehicleId()).thenReturn(5);
        when(workOrder.getVehicleId()).thenReturn(5);
        when(workOrder.getItems()).thenReturn(Arrays.asList(items));
        when(workOrderService.findById(1)).thenReturn(Optional.of(workOrder));
    }

    @Test
    @DisplayName("An unfinished work order cannot be invoiced")
    void workOrderNotCompletedThrows() {
        WorkOrder workOrder = mock(WorkOrder.class);
        when(workOrder.getStatus()).thenReturn(WorkOrderStatus.IN_PROGRESS);
        when(workOrder.getVehicleId()).thenReturn(5);
        when(workOrderService.findById(1)).thenReturn(Optional.of(workOrder));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> invoiceService.create(1, null));

        assertEquals("error.invoice.workOrderNotCompleted", ex.getMessageKey());
    }

    @Test
    @DisplayName("A work order can only be invoiced once")
    void alreadyInvoicedWorkOrderThrows() {
        givenWorkOrderWithPrices("100");
        when(invoiceRepo.existsByWorkOrderId(1)).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> invoiceService.create(1, null));

        assertEquals("error.invoice.alreadyInvoiced", ex.getMessageKey());
    }

    @Test
    @DisplayName("markPaid marks the invoice as paid and saves it")
    void markPaidMarksInvoiceAsPaid() {
        Invoice invoice = new Invoice(1, LocalDate.now());
        when(invoiceRepo.findById(3)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.markPaid(3);

        assertTrue(result.isPaid());
        verify(invoiceRepo).save(invoice);
    }

    @Test
    @DisplayName("markPaid throws when the invoice does not exist")
    void markPaidUnknownInvoiceThrows() {
        when(invoiceRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> invoiceService.markPaid(99));
    }

    @Test
    @DisplayName("markPaid rejects an invoice that is already paid")
    void markPaidAlreadyPaidThrows() {
        Invoice invoice = new Invoice(1, LocalDate.now());
        invoice.markPaid();
        when(invoiceRepo.findById(3)).thenReturn(Optional.of(invoice));

        assertThrows(ValidationException.class, () -> invoiceService.markPaid(3));
    }
}
