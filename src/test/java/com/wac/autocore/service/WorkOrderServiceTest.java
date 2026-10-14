package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WorkOrderServiceTest {
    private WorkOrderRepository workOrderRepository;
    private BookingRepository bookingRepository;
    private MechanicRepository mechanicRepository;
    private VehicleRepo vehicleRepo;
    private ServiceItemRepository serviceItemRepository;

    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        workOrderRepository = mock(WorkOrderRepository.class);
        bookingRepository = mock(BookingRepository.class);
        mechanicRepository = mock(MechanicRepository.class);
        vehicleRepo = mock(VehicleRepo.class);
        serviceItemRepository = mock(ServiceItemRepository.class);

        workOrderService = new WorkOrderService(
                workOrderRepository,
                bookingRepository,
                mechanicRepository,
                vehicleRepo,
                serviceItemRepository
        );
    }

    @Test
    void createWorkOrder_shouldCreateWorkOrderFromBookedBooking() {
        Booking booking = mock(Booking.class);

        ServiceItem serviceItem = mock(ServiceItem.class);
        when(serviceItem.getId()).thenReturn(1);
        when(serviceItem.getName()).thenReturn("Oil Change");
        when(serviceItem.getPrice()).thenReturn(899.0);
        when(serviceItem.getEstimatedMinutes()).thenReturn(60);

        BookingServiceItem bookingItem = BookingServiceItem.snapshotOf(serviceItem);

        when(bookingRepository.findById(1))
                .thenReturn(Optional.of(booking));
        when(workOrderRepository.existsByBookingIdAndStatusNot(1, WorkOrderStatus.CANCELLED))
                .thenReturn(false);
        when(booking.getStatus())
                .thenReturn(BookingStatus.BOOKED);
        when(booking.getMechanicId())
                .thenReturn(5);
        when(mechanicRepository.existsById(5))
                .thenReturn(true);
        when(booking.getItems())
                .thenReturn(Collections.singletonList(bookingItem));
        when(booking.getId())
                .thenReturn(1);
        when(booking.getVehicleId())
                .thenReturn(10);
        when(workOrderRepository.save(any(WorkOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.createWorkOrder(1);

        assertNotNull(result);
        assertEquals(1, result.getBookingId());
        assertEquals(10, result.getVehicleId());
        assertEquals(5, result.getMechanicId());
        assertEquals(1, result.getItems().size());
        assertEquals("Oil Change", result.getItems().get(0).getServiceName());
        assertEquals(899.0, result.getItems().get(0).getAgreedPrice());
        assertInstanceOf(PlannedWorkOrder.class, result);
        assertEquals(WorkOrderStatus.CONFIRMED, result.getStatus());

        verify(bookingRepository).save(booking);
        verify(workOrderRepository).save(any(WorkOrder.class));
    }

    @Test
    void findById_shouldReturnEmptyWhenWorkOrderDoesNotExist() {
        when(workOrderRepository.findById(1))
                .thenReturn(Optional.empty());

        Optional<WorkOrder> result = workOrderService.findById(1);

        assertFalse(result.isPresent());
    }

    @Test
    void startWorkOrder_shouldStartAndSaveWorkOrder() {
        // Arrange
        WorkOrder workOrder = mock(WorkOrder.class);
        when(workOrder.getBookingId()).thenReturn(null);
        when(workOrderRepository.findById(1)).thenReturn(Optional.of(workOrder));

        // Act
        workOrderService.startWorkOrder(1);

        // Assert
        verify(workOrder).start();
        verify(workOrderRepository).save(workOrder);
    }

    @Test
    void startWorkOrder_shouldThrowWhenWorkOrderDoesNotExist() {
        // Arrange
        when(workOrderRepository.findById(99)).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> workOrderService.startWorkOrder(99));

        // Assert
        assertEquals("error.notFound", exception.getMessageKey());
        verify(workOrderRepository, never()).save(any());
    }
}
