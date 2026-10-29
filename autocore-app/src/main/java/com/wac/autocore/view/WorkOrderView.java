package com.wac.autocore.view;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderChange;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.MechanicService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderReopenResult;
import com.wac.autocore.service.WorkOrderScheduleResult;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.dialog.CreateWorkOrderDialog;
import com.wac.autocore.view.dialog.EditWorkOrderDialog;
import com.wac.autocore.view.dialog.QuickDraftWorkOrderDialog;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * <b>WorkOrderView</b>
 * <p>Responsibility: Shows and manages work orders in the user interface. Below the list,
 * the jobs for the selected work order are shown with name, duration and agreed price.</p>
 */
public class WorkOrderView extends BaseView {

    private static final String NO_VALUE = "-";

    private final TableView<WorkOrder> workOrderTable = new TableView<>();
    private final ObservableList<WorkOrder> masterData = FXCollections.observableArrayList();

    private final TableView<WorkOrderItem> jobTable = new TableView<>();
    private final ObservableList<WorkOrderItem> jobData = FXCollections.observableArrayList();
    private final Label jobSummaryLabel = new Label();

    private final Map<Integer, Booking> bookingsById = new HashMap<>();
    private final Map<Integer, Mechanic> mechanicsById = new HashMap<>();
    private final Map<Integer, Vehicle> vehiclesById = new HashMap<>();

    private final WorkOrderService workOrderService;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final BookingService bookingService;
    private final MechanicService mechanicService;
    private final ServiceItemService serviceItemService;

    private final Button btnQuickDraft = new Button(lang.get("btn.quickDraft"));
    private final Button btnConfirm = new Button(lang.get("btn.confirm"));
    private final Button btnStart = new Button(lang.get("btn.start"));
    private final Button btnComplete = new Button(lang.get("btn.complete"));
    private final Button btnCancel = new Button(lang.get("btn.cancel"));
    private final Button btnReopen = new Button(lang.get("btn.reopen"));
    private final Button btnCreate = new Button(lang.get("btn.createNew"));
    private final Button btnEdit = new Button(lang.get("btn.edit"));

    public WorkOrderView(WorkOrderService workOrderService,
                         CustomerService customerService,
                         VehicleService vehicleService,
                         BookingService bookingService,
                         MechanicService mechanicService,
                         ServiceItemService serviceItemService) {

        this.workOrderService = workOrderService;
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        this.bookingService = bookingService;
        this.mechanicService = mechanicService;
        this.serviceItemService = serviceItemService;
        refreshData();
        initializeTable();
        initializeJobTable();

        // The selection in the list controls both the buttons and which jobs are shown
        workOrderTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    updateButtonStates(newValue);
                    showJobs(newValue);
                }
        );

        updateButtonStates(null);
        showJobs(null);

        initView();
    }

    @Override
    protected String getTitleKey() {
        return "workOrder.title";
    }

    @Override
    protected void buildContent() {
        VBox.setVgrow(workOrderTable, Priority.ALWAYS);
        getChildren().addAll(createButtonBar(), workOrderTable, createJobSection());
    }

    private void refreshData() {
        bookingsById.clear();
        for (Booking booking : bookingService.listAll()) {
            bookingsById.put(booking.getId(), booking);
        }

        mechanicsById.clear();
        for (Mechanic mechanic : mechanicService.listAll()) {
            mechanicsById.put(mechanic.getId(), mechanic);
        }

        vehiclesById.clear();
        for (Vehicle vehicle : vehicleService.findAll()) {
            vehiclesById.put(vehicle.getId(), vehicle);
        }

        masterData.setAll(workOrderService.findAll());
    }

    private void initializeTable() {

        TableColumn<WorkOrder, String> idCol = new TableColumn<>(lang.get("table.workOrderId"));
        idCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId()))
        );

        // The registration number is enough to recognize the car at the counter
        TableColumn<WorkOrder, String> vehicleCol = new TableColumn<>(lang.get("table.vehicle"));
        vehicleCol.setCellValueFactory(c ->
                new SimpleStringProperty(vehicleName(c.getValue().getVehicleId()))
        );

        TableColumn<WorkOrder, String> bookingCol = new TableColumn<>(lang.get("table.bookingTask"));
        bookingCol.setCellValueFactory(c ->
                new SimpleStringProperty(displayName(c.getValue().getBookingId(), bookingsById, Booking::getDescription))
        );

        TableColumn<WorkOrder, String> mechanicCol = new TableColumn<>(lang.get("table.mechanic"));
        mechanicCol.setCellValueFactory(c ->
                new SimpleStringProperty(mechanicName(c.getValue().getMechanicId()))
        );

        // Planned orders have no description; line breaks are shown as spaces in the table
        TableColumn<WorkOrder, String> problemCol = new TableColumn<>(lang.get("table.problemDescription"));
        problemCol.setCellValueFactory(c -> {
            String description = c.getValue().getProblemDescription();
            if (description == null || description.trim().isEmpty()) {
                return new SimpleStringProperty(NO_VALUE);
            }
            return new SimpleStringProperty(description.trim().replaceAll("\\s*\\R\\s*", " "));
        });

        TableColumn<WorkOrder, String> itemCountCol = new TableColumn<>(lang.get("table.itemCount"));
        itemCountCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getItems().size()))
        );

        // Warranty claims show which order they concern, e.g. "Warranty claim for #12"
        TableColumn<WorkOrder, String> typeCol = new TableColumn<>(lang.get("table.type"));
        typeCol.setCellValueFactory(c -> {
            WorkOrder order = c.getValue();
            Integer originalId = order.getOriginalWorkOrderId();
            if (originalId != null) {
                return new SimpleStringProperty(lang.get("workOrder.warrantyFor", originalId));
            }
            return new SimpleStringProperty(lang.get(order.getType().getMessageKey()));
        });

        // The status is stored as CONFIRMED/IN_PROGRESS/COMPLETED/DRAFT, only the display is translated
        TableColumn<WorkOrder, String> statusCol = new TableColumn<>(lang.get("table.status"));
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(lang.get("workOrder.status." + c.getValue().getStatus()))
        );

        problemCol.setPrefWidth(300);

        workOrderTable.getColumns().addAll(
                idCol, typeCol, vehicleCol, bookingCol, mechanicCol, problemCol, itemCountCol, statusCol
        );

        workOrderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        workOrderTable.setPlaceholder(new Label(lang.get("table.empty")));
        workOrderTable.setItems(masterData);
    }

    // The jobs are shown from the order's snapshot: what was agreed at booking time
    private void initializeJobTable() {

        TableColumn<WorkOrderItem, String> nameCol = new TableColumn<>(lang.get("table.name"));
        nameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getServiceName())
        );

        TableColumn<WorkOrderItem, String> durationCol = new TableColumn<>(lang.get("table.estimatedDuration"));
        durationCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getDurationMinutes()))
        );

        TableColumn<WorkOrderItem, Money> priceCol = new TableColumn<>(lang.get("table.price"));
        priceCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getAgreedPrice())
        );
        DialogUtil.configureMoneyColumn(priceCol);

        jobTable.getColumns().addAll(nameCol, durationCol, priceCol);
        jobTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        jobTable.setPlaceholder(new Label(lang.get("workOrder.selectForJobs")));
        jobTable.setPrefHeight(180);
        jobTable.setItems(jobData);
    }

    private VBox createJobSection() {
        Label jobTitle = new Label(lang.get("workOrder.jobs"));
        jobTitle.getStyleClass().add("text-header");

        jobSummaryLabel.getStyleClass().add("text-secondary");

        VBox box = new VBox(jobTitle, jobTable, jobSummaryLabel);
        box.getStyleClass().add("section");
        return box;
    }

    private void showJobs(WorkOrder selected) {
        if (selected == null) {
            jobData.clear();
            jobSummaryLabel.setText("");
            return;
        }

        jobData.setAll(selected.getItems());
        jobSummaryLabel.setText(
                lang.get("workOrder.total", selected.getTotalPrice())
                        + "  |  "
                        + lang.get("workOrder.totalDuration", selected.getTotalDurationMinutes())
        );
    }

    // The dialog lets the user choose a type (planned, drop-in or warranty claim) and create the work order
    private void openCreateWorkOrderDialog() {
        CreateWorkOrderDialog dialog = new CreateWorkOrderDialog(
                workOrderService.findBookableBookings(),
                bookingService.listAll(),
                workOrderService.findCompletedWorkOrders(),
                serviceItemService.listAll(),
                customerService,
                vehicleService
        );

        dialog.showAndWait().ifPresent(result -> {
            try {
                WorkOrderScheduleResult schedule;
                switch (result.getType()) {
                    case PLANNED:
                        schedule = workOrderService.createWorkOrder(result.getBooking().getId());
                        break;
                    case DROP_IN:
                        schedule = WorkOrderScheduleResult.withoutConflicts(workOrderService.createDropIn(
                                result.getVehicle().getId(),
                                result.getProblemDescription(),
                                result.getServiceItemIds()
                        ));
                        break;
                    case WARRANTY:
                        schedule = WorkOrderScheduleResult.withoutConflicts(workOrderService.createWarranty(
                                result.getOriginalWorkOrder().getId(),
                                result.getProblemDescription()
                        ));
                        break;
                    default:
                        throw new IllegalStateException("Unknown work order type: " + result.getType());
                }
                refreshAfterAction(schedule.getWorkOrder().getId());
                AlertHelper.showInfo(lang.get("workOrder.created"), lang.get("workOrder.createdMsg"));
                warnAboutMechanicConflicts(schedule);
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
            }
        });
    }

    private HBox createButtonBar() {
        setupButton(btnQuickDraft, "btn-primary", this::openQuickDraftDialog);
        setupButton(btnConfirm, "btn-secondary", () -> runAction(workOrderService::confirmWorkOrder, "workOrder.confirmed", "workOrder.confirmedMsg"));
        setupButton(btnStart, "btn-secondary", () -> runAction(workOrderService::startWorkOrder, "workOrder.started", "workOrder.startedMsg"));
        setupButton(btnComplete, "btn-secondary", () -> runAction(workOrderService::completeWorkOrder, "workOrder.completed", "workOrder.completedMsg"));
        setupButton(btnCancel, "btn-danger", this::cancelSelectedWorkOrder);
        setupButton(btnReopen, "btn-secondary", this::reopenSelectedWorkOrder);
        setupButton(btnCreate, "btn-primary", this::openCreateWorkOrderDialog);
        setupButton(btnEdit, "btn-secondary", this::openEditWorkOrderDialog);

        HBox toolbar = new HBox(btnQuickDraft, btnConfirm, btnStart, btnComplete, btnCancel, btnReopen, btnCreate, btnEdit);
        toolbar.getStyleClass().add("toolbar");
        return toolbar;
    }

    private void openQuickDraftDialog() {
        QuickDraftWorkOrderDialog dialog =
                new QuickDraftWorkOrderDialog(workOrderService, customerService, vehicleService);

        dialog.showAndWait().ifPresent(draft -> {
            refreshData();
            selectWorkOrder(draft.getId());
        });
    }

    private void selectWorkOrder(int id) {
        masterData.stream()
                .filter(workOrder -> workOrder.getId() == id)
                .findFirst()
                .ifPresent(workOrder -> {
                    workOrderTable.getSelectionModel().select(workOrder);
                    workOrderTable.scrollTo(workOrder);
                });
    }

    private void updateButtonStates(WorkOrder selected) {
        enableIfAllowed(btnConfirm, selected, WorkOrderStatus.CONFIRMED);
        enableIfAllowed(btnStart, selected, WorkOrderStatus.IN_PROGRESS);
        enableIfAllowed(btnComplete, selected, WorkOrderStatus.COMPLETED);
        enableIfAllowed(btnCancel, selected, WorkOrderStatus.CANCELLED);
        enableIfAllowed(btnReopen, selected, WorkOrderStatus.DRAFT);
        btnEdit.setDisable(selected == null || !selected.getStatus().isEditable());
    }

    private void enableIfAllowed(Button button, WorkOrder selected, WorkOrderStatus target) {
        button.setDisable(selected == null || !selected.getStatus().canChangeTo(target));
    }

    // Common handling for all actions: runs the service, reloads the table and shows the result.
    // Errors (e.g. validation errors on Confirm) are shown via ErrorFacade.
    private void runAction(IntConsumer action, String titleKey, String messageKey) {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            action.accept(selected.getId());
            refreshAfterAction(selected.getId());
            AlertHelper.showInfo(lang.get(titleKey), lang.get(messageKey));
        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
            refreshAfterAction(selected.getId());
        }
    }

    // The table replaces all rows on refresh and the selection disappears. We select the same order again
    // so the buttons reflect the new status.
    private void refreshAfterAction(int workOrderId) {
        refreshData();
        selectWorkOrder(workOrderId);
    }

    private void cancelSelectedWorkOrder() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean confirmed = AlertHelper.showConfirmation(
                lang.get("workOrder.cancelConfirmTitle"),
                lang.get("workOrder.cancelConfirmMsg", selected.getId()));
        if (confirmed) {
            runAction(workOrderService::cancelWorkOrder, "workOrder.cancelled", "workOrder.cancelledMsg");
        }
    }

    // A cancelled planned order becomes a new order from the booking. The new order is selected and the differences
    // from the cancelled one are shown. Other orders become drafts, as before.
    private void reopenSelectedWorkOrder() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            WorkOrderReopenResult result = workOrderService.reopenWorkOrder(selected.getId());
            int reopenedId = result.getWorkOrder().getId();
            refreshAfterAction(reopenedId);
            if (reopenedId == selected.getId()) {
                AlertHelper.showInfo(lang.get("workOrder.reopened"), lang.get("workOrder.reopenedMsg"));
            } else {
                AlertHelper.showInfo(lang.get("workOrder.recreated"), recreatedMessage(reopenedId, result.getChanges()));
            }
            warnAboutMechanicConflicts(result.getSchedule());
        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
            refreshAfterAction(selected.getId());
        }
    }

    private String recreatedMessage(int workOrderId, List<WorkOrderChange> changes) {
        StringBuilder sb = new StringBuilder(lang.get("workOrder.recreatedMsg", workOrderId));
        if (!changes.isEmpty()) {
            sb.append("\n\n").append(lang.get("workOrder.changesSinceCancelled"));
            for (WorkOrderChange change : changes) {
                sb.append("\n• ").append(describe(change));
            }
        }
        return sb.toString();
    }

    // The text per change type is in the language files as workOrder.change.<type>
    private String describe(WorkOrderChange change) {
        String key = "workOrder.change." + change.getType().name();
        switch (change.getType()) {
            case VEHICLE_CHANGED:
                return lang.get(key, vehicleName(change.getPreviousId()), vehicleName(change.getCurrentId()));
            case MECHANIC_CHANGED:
                return lang.get(key, mechanicName(change.getPreviousId()), mechanicName(change.getCurrentId()));
            case DATE_CHANGED:
                return lang.get(key, orNoValue(change.getPreviousDate()), orNoValue(change.getCurrentDate()));
            case START_TIME_CHANGED:
                return lang.get(key, orNoValue(change.getPreviousStartTime()), orNoValue(change.getCurrentStartTime()));
            case PRICE_CHANGED:
                return lang.get(key, change.getServiceName(),
                        change.getPreviousItem().getAgreedPrice(), change.getCurrentItem().getAgreedPrice());
            case DURATION_CHANGED:
                return lang.get(key, change.getServiceName(),
                        change.getPreviousItem().getDurationMinutes(), change.getCurrentItem().getDurationMinutes());
            default:
                return lang.get(key, change.getServiceName());
        }
    }

    // ISO format (2026-11-02, 08:00) as in the rest of the view. A missing value is shown as NO_VALUE.
    private static String orNoValue(Object value) {
        return value == null ? NO_VALUE : value.toString();
    }

    private String vehicleName(Integer vehicleId) {
        return displayName(vehicleId, vehiclesById, Vehicle::getRegistrationNumber);
    }

    private String mechanicName(Integer mechanicId) {
        return displayName(mechanicId, mechanicsById, Mechanic::getName);
    }

    // Shows an id from a lookup table: null → NO_VALUE, unknown id → common.unknownId, otherwise the display value
    private <T> String displayName(Integer id, Map<Integer, T> byId, Function<? super T, String> display) {
        if (id == null) {
            return NO_VALUE;
        }
        T item = byId.get(id);
        return item != null ? display.apply(item) : lang.get("common.unknownId", id);
    }

    private void openEditWorkOrderDialog() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        openEditWorkOrderDialog(selected, selected.getPlannedStartTime());
    }

    // The dialog saves by itself and shows errors (e.g. a clash on a confirmed order) without closing.
    // A saved draft can still clash, and then the warning is shown here.
    private void openEditWorkOrderDialog(WorkOrder workOrder, LocalTime startTime) {
        EditWorkOrderDialog dialog = new EditWorkOrderDialog(
                workOrder, serviceItemService.listAll(), mechanicService.listAll(), workOrderService, startTime);

        dialog.showAndWait().ifPresent(schedule -> {
            refreshAfterAction(workOrder.getId());
            AlertHelper.showInfo(lang.get("workOrder.updated"), lang.get("workOrder.updatedMsg"));
            warnAboutMechanicConflicts(schedule);
        });
    }

    // Common warning for creating, editing and reopening. The order is already saved as a draft;
    // the user chooses to change mechanic/time (the dialog opens with the suggested time) or to keep it.
    private void warnAboutMechanicConflicts(WorkOrderScheduleResult schedule) {
        if (schedule.getMechanicConflicts().isEmpty()) {
            return;
        }

        WorkOrder workOrder = schedule.getWorkOrder();
        StringBuilder sb = new StringBuilder(lang.get("workOrder.conflict.msg",
                mechanicName(workOrder.getMechanicId()), workOrder.getPlannedDate()));
        for (WorkOrder conflict : schedule.getMechanicConflicts()) {
            sb.append("\n• ").append(lang.get("workOrder.conflict.item",
                    String.valueOf(conflict.getId()), conflict.getPlannedStartTime(), conflict.getPlannedEndTime()));
        }
        sb.append("\n\n").append(schedule.getSuggestedStartTime()
                .map(time -> lang.get("workOrder.conflict.suggestion", time))
                .orElse(lang.get("workOrder.conflict.noSuggestion")));
        sb.append("\n\n").append(lang.get("workOrder.conflict.draftSaved"));

        boolean change = AlertHelper.showWarningChoice(lang.get("workOrder.conflict.title"), sb.toString(),
                lang.get("btn.changeMechanicOrTime"), lang.get("btn.keep"));
        if (change) {
            openEditWorkOrderDialog(workOrder,
                    schedule.getSuggestedStartTime().orElse(workOrder.getPlannedStartTime()));
        }
    }

    // Every toolbar button gets the shared .btn class plus its variant (primary, secondary or danger)
    private void setupButton(Button button, String variant, Runnable action) {
        button.getStyleClass().addAll("btn", variant);
        button.setOnAction(event -> action.run());
    }
}
