package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.WorkOrderLockedException;
import com.wac.autocore.model.*;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WorkOrderServiceTest {
    // An unsaved booking has id 0, so the order created from it gets bookingId 0.
    private static final int UNSAVED_BOOKING_ID = 0;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);
    private static final LocalTime EIGHT = LocalTime.of(8, 0);
    private static final Mechanic MECHANIC = new Mechanic("Ali", "070-1234567", "Engine");

    private WorkOrderRepository workOrderRepository;
    private BookingRepository bookingRepository;
    private MechanicRepository mechanicRepository;
    private VehicleRepo vehicleRepo;
    private ServiceItemRepository serviceItemRepository;
    private BookingService bookingService;

    private WorkOrderEventPublisher recordingPublisher;
    private List<WorkOrderEvent> publishedEvents;

    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        recordingPublisher = publishedEvents::add;

        workOrderRepository = mock(WorkOrderRepository.class);
        bookingRepository = mock(BookingRepository.class);
        mechanicRepository = mock(MechanicRepository.class);
        vehicleRepo = mock(VehicleRepo.class);
        serviceItemRepository = mock(ServiceItemRepository.class);
        bookingService = mock(BookingService.class);
        // The lookups are default methods on the repository; the mock should run them against the stubbed findById.
        when(serviceItemRepository.requireById(anyInt())).thenCallRealMethod();
        when(serviceItemRepository.requireAllById(any())).thenCallRealMethod();

        workOrderService = new WorkOrderService(
                workOrderRepository,
                bookingRepository,
                mechanicRepository,
                vehicleRepo,
                serviceItemRepository,
                bookingService,
                recordingPublisher
        );
    }

    @Test
    void createWorkOrder_shouldCreateWorkOrderFromBookedBooking() {
        stubBookedBooking(1, null);

        WorkOrderScheduleResult result = workOrderService.createWorkOrder(1);

        WorkOrder order = result.getWorkOrder();
        assertNotNull(order);
        assertEquals(1, order.getBookingId());
        assertEquals(10, order.getVehicleId());
        assertNull(order.getMechanicId());
        assertEquals(DATE, order.getPlannedDate());
        assertEquals(EIGHT, order.getPlannedStartTime());
        assertEquals(1, order.getItems().size());
        assertEquals("Oil Change", order.getItems().get(0).getServiceName());
        assertMoney(Money.of(899), order.getItems().get(0).getAgreedPrice());
        assertInstanceOf(PlannedWorkOrder.class, order);
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());
        assertTrue(result.getMechanicConflicts().isEmpty());
        assertFalse(result.getSuggestedStartTime().isPresent());

        verify(bookingService).followWorkOrderStatus(1, WorkOrderStatus.DRAFT);
        verify(workOrderRepository).save(order);
        verify(workOrderRepository, never()).findByMechanicIdAndPlannedDate(anyInt(), any());
        verifyNoInteractions(mechanicRepository);
    }

    @Test
    void createWorkOrder_suggestedMechanicIsBusy_savesDraftAndWarnsWithNextFreeTime() {
        stubBookedBooking(1, 7);
        WorkOrder busy = confirmed(dropIn(2, 7, LocalTime.of(8, 30)));
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(busy));

        WorkOrderScheduleResult result = workOrderService.createWorkOrder(1);

        WorkOrder order = result.getWorkOrder();
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());
        assertEquals(Integer.valueOf(7), order.getMechanicId());
        assertEquals(Collections.singletonList(busy), result.getMechanicConflicts());
        assertEquals(Optional.of(LocalTime.of(9, 30)), result.getSuggestedStartTime());
        assertEventEquals(order, WorkOrderEvent.WorkOrderEventAction.CREATED);
        verify(workOrderRepository).save(order);
        verifyNoInteractions(mechanicRepository);
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
        assertEventEquals(workOrder, WorkOrderEvent.WorkOrderEventAction.STARTED);

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

    @Test
    void updateWorkOrder_shouldThrowLockedWhenWorkOrderIsInProgress() {
        // Arrange
        ServiceItem serviceItem = mock(ServiceItem.class);
        when(serviceItem.getId()).thenReturn(1);
        when(serviceItem.getName()).thenReturn("Oil Change");
        when(serviceItem.getPrice()).thenReturn(Money.of(899));
        when(serviceItem.getEstimatedMinutes()).thenReturn(60);

        DropInWorkOrder workOrder = DropInWorkOrder.draft(10, "Shakes when braking");
        workOrder.addItem(serviceItem);
        workOrder.setMechanicId(5);
        workOrder.confirm();
        workOrder.start();

        when(workOrderRepository.findById(1)).thenReturn(Optional.of(workOrder));
        when(mechanicRepository.findById(5)).thenReturn(Optional.of(MECHANIC));
        when(serviceItemRepository.findById(1)).thenReturn(Optional.of(serviceItem));

        // Act + Assert
        assertThrows(WorkOrderLockedException.class,
                () -> workOrderService.updateWorkOrder(
                        1,
                        Collections.singletonList(1),
                        5,
                        null,
                        null,
                        null,
                        null,
                        Collections.emptyMap()));

        verify(workOrderRepository, never()).save(any());
    }

    // ----- Services on update -----

    @Test
    void updateWorkOrder_keepsExistingItems_addsNewAtCatalogPrice_removesDeselected() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        draft.addItem(service(2, "Brake inspection", "1295.00", 60));
        stubOrder(draft);
        ServiceItem raisedOilChange = service(1, "Oil change", "999.00", 60);
        ServiceItem diagnostics = service(3, "Diagnostics", "598.50", 30);
        when(serviceItemRepository.findById(1)).thenReturn(Optional.of(raisedOilChange));
        when(serviceItemRepository.findById(3)).thenReturn(Optional.of(diagnostics));

        workOrderService.updateWorkOrder(1, Arrays.asList(3, 1), null, DATE, EIGHT,
                null, null, Collections.emptyMap());

        // The sequence follows the selection, not the work order's previous sequence.
        assertEquals(Arrays.asList(3, 1), draft.getItems().stream()
                .map(WorkOrderItem::getServiceItemId).collect(Collectors.toList()));
        assertMoney(Money.of("899.00"), itemFor(draft, 1).getAgreedPrice());
        assertMoney(Money.of("598.50"), itemFor(draft, 3).getAgreedPrice());
        assertEquals(LocalTime.of(9, 30), draft.getPlannedEndTime());

        verify(workOrderRepository).save(draft);
    }

    @Test
    void updateWorkOrder_unknownService_throwsAndDoesNotSave() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        stubOrder(draft);
        when(serviceItemRepository.findById(99)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> workOrderService.updateWorkOrder(1, Arrays.asList(1, 99), null, DATE, EIGHT,
                        null, null, Collections.emptyMap()));

        assertEquals("error.serviceNotFound", exception.getMessageKey());
        verify(workOrderRepository, never()).save(any());
    }

    // ----- The booking follows the order (Card proposal D) -----

    @Test
    void updateWorkOrder_plannedOrder_bookingFollowsDateTimeAndServices() {
        WorkOrder order = planned(1, 20);
        stubOrder(order);
        ServiceItem diagnostics = service(3, "Diagnostics", "598.50", 30);
        when(serviceItemRepository.findById(3)).thenReturn(Optional.of(diagnostics));
        LocalDate newDate = DATE.plusDays(1);

        workOrderService.updateWorkOrder(1, Arrays.asList(1, 3), null, newDate, LocalTime.of(13, 0),
                null, null, Collections.emptyMap());

        verify(bookingService).followWorkOrder(20, newDate, LocalTime.of(13, 0), Arrays.asList(1, 3));
        verify(workOrderRepository).save(order);
    }

    @Test
    void updateWorkOrder_dropIn_doesNotTouchAnyBooking() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        stubOrder(draft);

        update(draft, null, EIGHT);

        verifyNoInteractions(bookingService);
        verify(workOrderRepository).save(draft);
    }

    @Test
    void updateWorkOrder_bookingCannotFollow_throwsAndDoesNotSaveOrder() {
        WorkOrder order = planned(1, 20);
        stubOrder(order);
        ValidationException vehicleBooked = new ValidationException("error.vehicleBooked", "ABC123");
        doThrow(vehicleBooked).when(bookingService)
                .followWorkOrder(20, DATE.plusDays(1), EIGHT, Collections.singletonList(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> workOrderService.updateWorkOrder(1, Collections.singletonList(1), null, DATE.plusDays(1), EIGHT,
                        null, null, Collections.emptyMap()));

        assertSame(vehicleBooked, exception);
        verify(workOrderRepository, never()).save(any());
    }

    // ----- The mechanic's reservation (Card proposal M) -----

    @Test
    void updateWorkOrder_draftWithBusyMechanic_savesAndWarns() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        WorkOrder busy = confirmed(dropIn(2, 7, LocalTime.of(8, 30)));
        stubOrder(draft);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(busy));

        WorkOrderScheduleResult result = update(draft, 7, EIGHT);

        assertSame(draft, result.getWorkOrder());
        assertEquals(Integer.valueOf(7), draft.getMechanicId());
        assertEquals(Collections.singletonList(busy), result.getMechanicConflicts());
        assertEquals(Optional.of(LocalTime.of(9, 30)), result.getSuggestedStartTime());
        verify(workOrderRepository).save(draft);
    }

    @Test
    void updateWorkOrder_draftWithFreeMechanic_savesWithoutWarning() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        WorkOrder elsewhere = confirmed(dropIn(2, 7, LocalTime.of(12, 0)));
        stubOrder(draft);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(elsewhere));

        WorkOrderScheduleResult result = update(draft, 7, EIGHT);

        assertTrue(result.getMechanicConflicts().isEmpty());
        assertFalse(result.getSuggestedStartTime().isPresent());
        verify(workOrderRepository).save(draft);
    }

    @Test
    void updateWorkOrder_withoutMechanic_skipsMechanicLookupAndConflictCheck() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        stubOrder(draft);

        WorkOrderScheduleResult result = update(draft, null, EIGHT);

        assertNull(draft.getMechanicId());
        assertTrue(result.getMechanicConflicts().isEmpty());
        verifyNoInteractions(mechanicRepository);
        verify(workOrderRepository, never()).findByMechanicIdAndPlannedDate(anyInt(), any());
        verify(workOrderRepository).save(draft);
    }

    @Test
    void updateWorkOrder_unknownMechanic_throwsAndDoesNotSave() {
        WorkOrder draft = dropIn(1, null, EIGHT);
        when(workOrderRepository.findById(1)).thenReturn(Optional.of(draft));
        when(mechanicRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> workOrderService.updateWorkOrder(1,
                Collections.singletonList(1), 99, DATE, EIGHT, null, null, Collections.emptyMap()));
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void updateWorkOrder_confirmedOrderToBusyMechanic_throwsAndDoesNotSave() {
        WorkOrder order = confirmed(dropIn(1, 5, EIGHT));
        WorkOrder busy = confirmed(dropIn(2, 7, LocalTime.of(8, 30)));
        stubOrder(order);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(busy));

        MechanicDoubleBookingException error = assertThrows(MechanicDoubleBookingException.class,
                () -> update(order, 7, EIGHT));

        // The message goes to the log, so it must identify the mechanic by id, not by name
        assertFalse(error.getMessage().contains(MECHANIC.getName()));
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void updateWorkOrder_confirmedOrderToBusyTime_throwsAndDoesNotSave() {
        WorkOrder order = confirmed(dropIn(1, 7, LocalTime.of(12, 0)));
        WorkOrder busy = confirmed(dropIn(2, 7, LocalTime.of(8, 30)));
        stubOrder(order);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Arrays.asList(order, busy));

        assertThrows(MechanicDoubleBookingException.class, () -> update(order, 7, EIGHT));
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void confirmWorkOrder_withBusyMechanic_throwsAndDoesNotSave() {
        WorkOrder draft = dropIn(1, 7, EIGHT);
        WorkOrder busy = confirmed(dropIn(2, 7, LocalTime.of(8, 30)));
        stubOrder(draft);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(busy));

        MechanicDoubleBookingException error = assertThrows(MechanicDoubleBookingException.class,
                () -> workOrderService.confirmWorkOrder(1));

        assertTrue(publishedEvents.isEmpty());

        assertEquals(7, error.getMechanicId());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void confirmWorkOrder_withFreeMechanic_confirmsAndSaves() {
        WorkOrder draft = dropIn(1, 7, EIGHT);
        stubOrder(draft);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(draft));

        workOrderService.confirmWorkOrder(1);

        assertEventEquals(draft, WorkOrderEvent.WorkOrderEventAction.CONFIRMED);

        assertEquals(WorkOrderStatus.CONFIRMED, draft.getStatus());
        verify(workOrderRepository).save(draft);
    }

    @Test
    void twoDraftsWithSameMechanicAndTime_bothSave_secondIsStoppedAtConfirm() {
        WorkOrder first = dropIn(1, null, EIGHT);
        WorkOrder second = dropIn(2, null, EIGHT);
        stubOrder(first);
        stubOrder(second);
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Arrays.asList(first, second));

        assertTrue(update(first, 7, EIGHT).getMechanicConflicts().isEmpty());
        assertTrue(update(second, 7, EIGHT).getMechanicConflicts().isEmpty());

        workOrderService.confirmWorkOrder(1);
        assertThrows(MechanicDoubleBookingException.class, () -> workOrderService.confirmWorkOrder(2));

        assertEquals(WorkOrderStatus.CONFIRMED, first.getStatus());
        verify(workOrderRepository, times(1)).save(second);
    }

    // ----- reopenWorkOrder -----

    @Test
    void reopenWorkOrder_cancelledPlannedOrder_createsNewDraftFromCurrentBooking() {
        ServiceItem oil = service(1, "Oil change", "899.00", 45);
        ServiceItem brakes = service(2, "Brake inspection", "1295.00", 60);
        Booking booking = new Booking(10, 5, DATE, EIGHT, "Service", Collections.singletonList(oil));

        WorkOrder cancelled = PlannedWorkOrder.createFrom(booking);
        cancelled.setMechanicId(3);
        cancelled.setComments("The customer is waiting on site");
        cancelled.setCustomerInstructions("Call before starting");
        cancelled.cancel();

        // The booking is changed while the order is cancelled: one more service
        booking.update(10, 5, DATE, EIGHT, "Service", Arrays.asList(oil, brakes));
        stubReopen(cancelled, booking);

        WorkOrderReopenResult result = workOrderService.reopenWorkOrder(1);

        WorkOrder reopened = result.getWorkOrder();
        assertNotSame(cancelled, reopened);
        assertInstanceOf(PlannedWorkOrder.class, reopened);
        assertEquals(WorkOrderStatus.DRAFT, reopened.getStatus());
        assertEquals(Integer.valueOf(5), reopened.getMechanicId(),
                "The new order gets the booking's suggestion, not the cancelled order's mechanic");
        assertEquals(2, reopened.getItems().size());
        assertEquals("The customer is waiting on site", reopened.getComments());
        assertEquals("Call before starting", reopened.getCustomerInstructions());

        assertEquals(WorkOrderStatus.CANCELLED, cancelled.getStatus());

        assertEquals(Arrays.asList(WorkOrderChangeType.MECHANIC_CHANGED, WorkOrderChangeType.SERVICE_ADDED),
                result.getChanges().stream().map(WorkOrderChange::getType).collect(Collectors.toList()));
        assertTrue(result.getMechanicConflicts().isEmpty());

        assertEventEquals(reopened, WorkOrderEvent.WorkOrderEventAction.CREATED);

        verify(workOrderRepository).save(reopened);
        verify(workOrderRepository, never()).save(cancelled);
        verify(bookingService).followWorkOrderStatus(UNSAVED_BOOKING_ID, WorkOrderStatus.DRAFT);
    }

    @Test
    void reopenWorkOrder_cancelledPlannedOrder_warnsWhenSuggestedMechanicIsBusy() {
        Booking booking = new Booking(10, 7, DATE, EIGHT, "Service",
                Collections.singletonList(service(1, "Oil change", "899.00", 45)));
        WorkOrder cancelled = PlannedWorkOrder.createFrom(booking);
        cancelled.cancel();
        stubReopen(cancelled, booking);
        WorkOrder busy = confirmed(dropIn(2, 7, LocalTime.of(8, 30)));
        when(workOrderRepository.findByMechanicIdAndPlannedDate(7, DATE)).thenReturn(Collections.singletonList(busy));

        WorkOrderReopenResult result = workOrderService.reopenWorkOrder(1);

        assertEquals(WorkOrderStatus.DRAFT, result.getWorkOrder().getStatus());
        assertEquals(Collections.singletonList(busy), result.getMechanicConflicts());
        assertEquals(Optional.of(LocalTime.of(9, 30)), result.getSuggestedStartTime());

        verify(workOrderRepository).save(result.getWorkOrder());
    }

    @Test
    void reopenWorkOrder_cancelledPlannedOrder_throwsWhenBookingHasAnotherActiveOrder() {
        Booking booking = new Booking(10, 5, DATE, EIGHT, "Service",
                Collections.singletonList(service(1, "Oil change", "899.00", 45)));
        WorkOrder cancelled = PlannedWorkOrder.createFrom(booking);
        cancelled.cancel();

        when(workOrderRepository.findById(1)).thenReturn(Optional.of(cancelled));
        when(workOrderRepository.existsByBookingIdAndStatusNot(UNSAVED_BOOKING_ID, WorkOrderStatus.CANCELLED))
                .thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> workOrderService.reopenWorkOrder(1));

        assertEquals("error.workOrder.bookingHasActiveOrder", exception.getMessageKey());

        verify(workOrderRepository, never()).save(any());
        verifyNoInteractions(bookingService);
    }

    @Test
    void reopenWorkOrder_cancelledPlannedOrder_throwsWhenBookingIsCancelled() {
        Booking booking = new Booking(10, 5, DATE, EIGHT, "Service",
                Collections.singletonList(service(1, "Oil change", "899.00", 45)));
        WorkOrder cancelled = PlannedWorkOrder.createFrom(booking);
        cancelled.cancel();
        booking.followWorkOrder(cancelled.getStatus());
        booking.cancel();
        stubReopen(cancelled, booking);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> workOrderService.reopenWorkOrder(1));

        assertEquals("error.workOrder.bookingNotBookable", exception.getMessageKey());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(workOrderRepository, never()).save(any());
        verifyNoInteractions(bookingService);
    }

    @Test
    void reopenWorkOrder_confirmedOrder_becomesDraftWithoutChanges() {
        Booking booking = new Booking(10, 5, DATE, EIGHT, "Service",
                Collections.singletonList(service(1, "Oil change", "899.00", 45)));
        WorkOrder confirmed = confirmed(PlannedWorkOrder.createFrom(booking));

        when(workOrderRepository.findById(1)).thenReturn(Optional.of(confirmed));

        WorkOrderReopenResult result = workOrderService.reopenWorkOrder(1);

        assertEventEquals(confirmed, WorkOrderEvent.WorkOrderEventAction.REOPENED);

        assertEquals(1, publishedEvents.size());
        assertEventEquals(confirmed, WorkOrderEvent.WorkOrderEventAction.REOPENED);
        verify(workOrderRepository).save(confirmed);
    }

    @Test
    void reopenWorkOrder_cancelledDropIn_becomesDraftWithoutChanges() {
        DropInWorkOrder dropIn = DropInWorkOrder.draft(10, "Shakes when braking");
        dropIn.addItem(service(1, "Oil change", "899.00", 45));
        dropIn.cancel();

        when(workOrderRepository.findById(1)).thenReturn(Optional.of(dropIn));

        WorkOrderReopenResult result = workOrderService.reopenWorkOrder(1);

        assertSame(dropIn, result.getWorkOrder());
        assertEquals(WorkOrderStatus.DRAFT, dropIn.getStatus());
        assertTrue(result.getChanges().isEmpty());

        assertEventEquals(dropIn, WorkOrderEvent.WorkOrderEventAction.REOPENED);
        verify(workOrderRepository).save(dropIn);
    }

    @Test
    void reopenWorkOrder_completedOrder_throws() {
        Booking booking = new Booking(10, 5, DATE, EIGHT, "Service",
                Collections.singletonList(service(1, "Oil change", "899.00", 45)));
        WorkOrder completed = confirmed(PlannedWorkOrder.createFrom(booking));
        completed.start();
        completed.complete();

        when(workOrderRepository.findById(1)).thenReturn(Optional.of(completed));

        assertTrue(publishedEvents.isEmpty());

        assertThrows(IllegalStatusTransitionException.class, () -> workOrderService.reopenWorkOrder(1));
        verify(workOrderRepository, never()).save(any());
    }

    // Helper methods

    // A booked booking (mock) with one 60 min service at 08:00 and an optional suggested mechanic.
    private void stubBookedBooking(int bookingId, Integer suggestedMechanicId) {
        Booking booking = mock(Booking.class);
        // The snapshot is created before the stubbing: it calls the service mock, which Mockito does not allow inside when(...).
        List<BookingServiceItem> items = Collections.singletonList(
                BookingServiceItem.snapshotOf(service(1, "Oil Change", "899", 60)));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(workOrderRepository.existsByBookingIdAndStatusNot(bookingId, WorkOrderStatus.CANCELLED)).thenReturn(false);
        when(booking.getStatus()).thenReturn(BookingStatus.BOOKED);
        when(booking.getDate()).thenReturn(DATE);
        when(booking.getStartTime()).thenReturn(EIGHT);
        when(booking.getItems()).thenReturn(items);
        when(booking.getId()).thenReturn(bookingId);
        when(booking.getVehicleId()).thenReturn(10);
        when(booking.getMechanicId()).thenReturn(suggestedMechanicId);
        when(vehicleRepo.existsById(10)).thenReturn(true);
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Stubs for reopening a cancelled planned order (id 1) from an unsaved booking.
    private void stubReopen(WorkOrder cancelled, Booking booking) {
        when(workOrderRepository.findById(1)).thenReturn(Optional.of(cancelled));
        when(workOrderRepository.existsByBookingIdAndStatusNot(UNSAVED_BOOKING_ID, WorkOrderStatus.CANCELLED))
                .thenReturn(false);
        when(bookingRepository.findById(UNSAVED_BOOKING_ID)).thenReturn(Optional.of(booking));
        when(vehicleRepo.existsById(10)).thenReturn(true);
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private WorkOrderScheduleResult update(WorkOrder order, Integer mechanicId, LocalTime startTime) {
        if (mechanicId != null) {
            when(mechanicRepository.findById(mechanicId)).thenReturn(Optional.of(MECHANIC));
        }
        return workOrderService.updateWorkOrder(order.getId(), Collections.singletonList(1), mechanicId,
                DATE, startTime, null, null, Collections.emptyMap());
    }

    // Stubs the order and the catalog for service 1, which all helper orders have; the update looks up all selected services.
    private void stubOrder(WorkOrder order) {
        ServiceItem oilChange = service(1, "Oil change", "899.00", 60);
        when(workOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(serviceItemRepository.findById(1)).thenReturn(Optional.of(oilChange));
    }

    // Drop-in draft with one 60 min service. The id is set so the clash rule can tell the orders apart.
    private static WorkOrderItem itemFor(WorkOrder order, int serviceItemId) {
        return order.getItems().stream()
                .filter(item -> item.getServiceItemId() == serviceItemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Service " + serviceItemId + " is missing from the order"));
    }

    private static WorkOrder dropIn(int id, Integer mechanicId, LocalTime startTime) {
        DropInWorkOrder order = DropInWorkOrder.draft(10, "Shakes when braking");
        ReflectionTestUtils.setField(order, "id", id);
        order.addItem(service(1, "Oil change", "899.00", 60));
        order.setMechanicId(mechanicId);
        order.setPlannedDate(DATE);
        order.setPlannedStartTime(startTime);
        return order;
    }

    // Planned order (draft) from a saved booking with one 60 min service at 08:00.
    private static WorkOrder planned(int id, int bookingId) {
        Booking booking = new Booking(10, null, DATE, EIGHT, "Service",
                Collections.singletonList(service(1, "Oil change", "899.00", 60)));
        ReflectionTestUtils.setField(booking, "id", bookingId);
        WorkOrder order = PlannedWorkOrder.createFrom(booking);
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private static WorkOrder confirmed(WorkOrder order) {
        if (order.getMechanicId() == null) {
            order.setMechanicId(5);
        }
        order.confirm();
        return order;
    }

    private static ServiceItem service(int id, String name, String price, int minutes) {
        ServiceItem service = mock(ServiceItem.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn(name);
        when(service.getPrice()).thenReturn(Money.of(price));
        when(service.getEstimatedMinutes()).thenReturn(minutes);
        return service;
    }

    /**
     * Asserts that a {@link WorkOrder} has the correct and expected
     * {@link com.wac.autocore.model.WorkOrderEvent.WorkOrderEventAction}.
     *
     * Also asserts that the {@code publishedEvents} contains 1 element, and that the {@link WorkOrder}
     * is of the same {@code ID} as the {@code events} workOrderId.
     *
     * Finally, asserts that the occurredAt() of the {@link WorkOrderEvent} is not null.
     *
     * @param workOrder the {@link WorkOrder} to assert.
     * @param action the {@link com.wac.autocore.model.WorkOrderEvent.WorkOrderEventAction} to compare to.
     * **/
    private void assertEventEquals(WorkOrder workOrder, WorkOrderEvent.WorkOrderEventAction action) {
        assertEquals(1, publishedEvents.size());
        WorkOrderEvent event = publishedEvents.getFirst();
        assertEquals(workOrder.getId(), event.workOrderId());
        assertEquals(action, event.action());
        assertNotNull(event.occurredAt());
    }
}
