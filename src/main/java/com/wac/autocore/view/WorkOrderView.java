package com.wac.autocore.view;

import com.wac.autocore.model.*;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.dialog.CreateWorkOrderDialog;
import com.wac.autocore.view.dialog.QuickDraftWorkOrderDialog;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <b>WorkOrderView</b>
 * <p>Ansvar: Visar och hanterar arbetsordrar i användargränssnittet. Under listan visas
 * jobben för vald arbetsorder med namn, tid och avtalat pris.</p>
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

    private final Button btnStart = new Button(lang.get("btn.start"));
    private final Button btnComplete = new Button(lang.get("btn.complete"));
    private final Button btnCreate = new Button(lang.get("btn.createNew"));
    private final Button btnQuickDraft = new Button(lang.get("btn.quickDraft"));
    private final Button btnConfirm = new Button(lang.get("btn.confirm"));
    private final Button btnCancel = new Button(lang.get("btn.cancel"));
    private final Button btnReopen = new Button(lang.get("btn.reopen"));

    public WorkOrderView(WorkOrderService workOrderService,
                         CustomerService customerService,
                         VehicleService vehicleService) {
        this.workOrderService = workOrderService;
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        refreshData();
        initializeTable();
        initializeJobTable();

        // Valet i listan styr både knapparna och vilka jobb som visas
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
        for (Booking booking : workOrderService.findAllBookings()) {
            bookingsById.put(booking.getId(), booking);
        }

        mechanicsById.clear();
        for (Mechanic mechanic : workOrderService.findAllMechanics()) {
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

        // Regnummer räcker för att känna igen bilen vid disken
        TableColumn<WorkOrder, String> vehicleCol = new TableColumn<>(lang.get("table.vehicle"));
        vehicleCol.setCellValueFactory(c -> {
            Integer vehicleId = c.getValue().getVehicleId();
            if (vehicleId == null) {
                return new SimpleStringProperty(NO_VALUE);
            }
            Vehicle vehicle = vehiclesById.get(vehicleId);
            String display = vehicle != null
                    ? vehicle.getRegistrationNumber()
                    : lang.get("common.unknownId", vehicleId);
            return new SimpleStringProperty(display);
        });

        TableColumn<WorkOrder, String> bookingCol = new TableColumn<>(lang.get("table.bookingTask"));
        bookingCol.setCellValueFactory(c -> {
            Integer bookingId = c.getValue().getBookingId();
            if (bookingId == null) {
                return new SimpleStringProperty(NO_VALUE);
            }
            Booking booking = bookingsById.get(bookingId);
            String display = booking != null
                    ? booking.getDescription()
                    : lang.get("common.unknownId", bookingId);
            return new SimpleStringProperty(display);
        });

        TableColumn<WorkOrder, String> mechanicCol = new TableColumn<>(lang.get("table.mechanic"));
        mechanicCol.setCellValueFactory(c -> {
            Integer mechanicId = c.getValue().getMechanicId();
            if (mechanicId == null) {
                return new SimpleStringProperty(NO_VALUE);
            }
            Mechanic mechanic = mechanicsById.get(mechanicId);
            String display = mechanic != null
                    ? mechanic.getName()
                    : lang.get("common.unknownId", mechanicId);
            return new SimpleStringProperty(display);
        });

        // Planerade ordrar saknar beskrivning; radbrytningar visas som mellanslag i tabellen
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

        // Reklamationer visar vilken order de gäller, t.ex. "Reklamation för #12"
        TableColumn<WorkOrder, String> typeCol = new TableColumn<>(lang.get("table.type"));
        typeCol.setCellValueFactory(c -> {
            WorkOrder order = c.getValue();
            if (order instanceof WarrantyWorkOrder) {
                Integer originalId = ((WarrantyWorkOrder) order).getOriginalWorkOrderId();
                if (originalId != null) {
                    return new SimpleStringProperty(lang.get("workOrder.warrantyFor", originalId));
                }
            }
            return new SimpleStringProperty(lang.get(order.getType().getMessageKey()));
        });

        // Statusen sparas som CONFIRMED/IN_PROGRESS/COMPLETED/DRAFT, bara visningen översätts
        TableColumn<WorkOrder, String> statusCol = new TableColumn<>(lang.get("table.status"));
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(lang.get("workOrder.status." + c.getValue().getStatus()))
        );

        idCol.setMaxWidth(80);
        problemCol.setPrefWidth(300);

        workOrderTable.getColumns().addAll(
                idCol, typeCol, vehicleCol, bookingCol, mechanicCol, problemCol, itemCountCol, statusCol
        );

        workOrderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        workOrderTable.setPlaceholder(new Label(lang.get("table.empty")));
        workOrderTable.setItems(masterData);
    }

    // Jobben visas från orderns snapshot: det som avtalades vid bokningen
    private void initializeJobTable() {

        TableColumn<WorkOrderItem, String> nameCol = new TableColumn<>(lang.get("table.name"));
        nameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getServiceName())
        );

        TableColumn<WorkOrderItem, String> durationCol = new TableColumn<>(lang.get("table.estimatedDuration"));
        durationCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getDurationMinutes()))
        );

        TableColumn<WorkOrderItem, String> priceCol = new TableColumn<>(lang.get("table.price"));
        priceCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.2f", c.getValue().getAgreedPrice()))
        );

        jobTable.getColumns().addAll(nameCol, durationCol, priceCol);
        jobTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        jobTable.setPlaceholder(new Label(lang.get("workOrder.selectForJobs")));
        jobTable.setPrefHeight(180);
        jobTable.setItems(jobData);
    }

    private VBox createJobSection() {
        Label jobTitle = new Label(lang.get("workOrder.jobs"));
        jobTitle.getStyleClass().add("text-title");

        jobSummaryLabel.getStyleClass().add("text-secondary");

        VBox box = new VBox(8, jobTitle, jobTable, jobSummaryLabel);
        box.setAlignment(Pos.TOP_LEFT);
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

    // Dialogen låter användaren välja typ (planerad, drop-in eller reklamation) och skapa arbetsordern
    private void openCreateWorkOrderDialog() {
        CreateWorkOrderDialog dialog = new CreateWorkOrderDialog(
                workOrderService.findBookableBookings(),
                workOrderService.findAllBookings(),
                workOrderService.findCompletedWorkOrders(),
                workOrderService.findAllServiceItems(),
                customerService,
                vehicleService
        );

        dialog.showAndWait().ifPresent(result -> {
            try {
                switch (result.getType()) {
                    case PLANNED:
                        workOrderService.createWorkOrder(result.getBooking().getId());
                        break;
                    case DROP_IN:
                        workOrderService.createDropIn(
                                result.getVehicle().getId(),
                                result.getProblemDescription(),
                                result.getServiceItemIds()
                        );
                        break;
                    case WARRANTY:
                        workOrderService.createWarranty(
                                result.getOriginalWorkOrder().getId(),
                                result.getProblemDescription()
                        );
                        break;
                    default:
                        throw new IllegalStateException("Unknown work order type: " + result.getType());
                }
                refreshData();
                AlertHelper.showInfo(lang.get("workOrder.created"), lang.get("workOrder.createdMsg"));
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
            }
        });
    }

    private HBox createButtonBar() {
        String btnPrimary = "btn-primary";

        btnStart.getStyleClass().addAll("btn", btnPrimary);
        btnStart.setOnAction(event -> runAction(workOrderService::startWorkOrder, "workOrder.started", "workOrder.startedMsg"));

        btnComplete.getStyleClass().addAll("btn", btnPrimary);
        btnComplete.setOnAction(event -> runAction(workOrderService::completeWorkOrder, "workOrder.completed", "workOrder.completedMsg"));

        btnCreate.getStyleClass().addAll("btn", btnPrimary);
        btnCreate.setOnAction(event -> openCreateWorkOrderDialog());

        btnQuickDraft.getStyleClass().addAll("btn", btnPrimary);
        btnQuickDraft.setOnAction(event -> openQuickDraftDialog());

        btnConfirm.getStyleClass().addAll("btn", btnPrimary);
        btnConfirm.setOnAction(event -> runAction(workOrderService::confirmWorkOrder, "workOrder.confirmed", "workOrder.confirmedMsg"));

        btnCancel.getStyleClass().addAll("btn", "btn-secondary");
        btnCancel.setOnAction(event -> cancelSelectedWorkOrder());

        btnReopen.getStyleClass().addAll("btn", "btn-secondary");
        btnReopen.setOnAction(event -> runAction(workOrderService::reopenWorkOrder, "workOrder.reopened", "workOrder.reopenedMsg"));

        HBox box = new HBox(15, btnQuickDraft, btnConfirm, btnStart, btnComplete, btnCancel, btnReopen, btnCreate);
        box.setPadding(new Insets(15, 0, 0, 0));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
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
    }

    private void enableIfAllowed(Button button, WorkOrder selected, WorkOrderStatus target) {
        button.setDisable(selected == null || !selected.getStatus().canChangeTo(target));
    }

    // Gemensam hantering för alla åtgärder: kör servicen, laddar om tabellen och visar resultatet.
    // Fel (t.ex. valideringsfel vid Confirm) visas via ErrorFacade.
    private void runAction(Consumer<WorkOrder> action, String titleKey, String messageKey) {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            action.accept(selected);
            refreshAfterAction(selected.getId());
            AlertHelper.showInfo(lang.get(titleKey), lang.get(messageKey));
        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
            refreshAfterAction(selected.getId());
        }
    }

    // Tabellen byter ut alla rader vid refresh och markeringen försvinner. Vi markerar samma order igen
    // så att knapparna återspeglar den nya statusen.
    private void refreshAfterAction(int workOrderId) {
        refreshData();
        selectWorkOrder(workOrderId);
        updateButtonStates(workOrderTable.getSelectionModel().getSelectedItem());
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
}
