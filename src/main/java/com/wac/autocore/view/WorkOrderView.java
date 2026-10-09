package com.wac.autocore.view;

import com.wac.autocore.model.*;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.dialog.CreateWorkOrderDialog;
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

    private final WorkOrderService workOrderService;
    private final CustomerService customerService;
    private final VehicleService vehicleService;

    private final Button btnStart = new Button(lang.get("btn.start"));
    private final Button btnComplete = new Button(lang.get("btn.complete"));
    private final Button btnCreate = new Button(lang.get("btn.createNew"));

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

        masterData.setAll(workOrderService.findAll());
    }

    private void initializeTable() {

        TableColumn<WorkOrder, String> idCol = new TableColumn<>(lang.get("table.workOrderId"));
        idCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId()))
        );

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

        TableColumn<WorkOrder, String> itemCountCol = new TableColumn<>(lang.get("table.itemCount"));
        itemCountCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getItems().size()))
        );

        // Statusen sparas som CONFIRMED/IN_PROGRESS/COMPLETED, bara visningen översätts
        TableColumn<WorkOrder, String> statusCol = new TableColumn<>(lang.get("table.status"));
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(lang.get("workOrder.status." + c.getValue().getStatus()))
        );

        workOrderTable.getColumns().addAll(idCol, bookingCol, mechanicCol, itemCountCol, statusCol);
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
        btnStart.setOnAction(event -> startSelectedWorkOrder());

        btnComplete.getStyleClass().addAll("btn", btnPrimary);
        btnComplete.setOnAction(event -> completeSelectedWorkOrder());

        btnCreate.getStyleClass().addAll("btn", btnPrimary);
        btnCreate.setOnAction(event -> openCreateWorkOrderDialog());

        HBox box = new HBox(15, btnStart, btnComplete, btnCreate);
        box.setPadding(new Insets(15, 0, 0, 0));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void updateButtonStates(WorkOrder selected) {
        if (selected == null) {
            btnStart.setDisable(true);
            btnComplete.setDisable(true);
            return;
        }
        WorkOrderStatus status = selected.getStatus();
        btnStart.setDisable(!status.equals(WorkOrderStatus.CONFIRMED));
        btnComplete.setDisable(!status.equals(WorkOrderStatus.IN_PROGRESS));
    }

    private void startSelectedWorkOrder() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            workOrderService.startWorkOrder(selected);
            refreshData();

            AlertHelper.showInfo(lang.get(
                    "workOrder.started"),
                    lang.get("workOrder.startedMsg"));

        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
        }
    }

    private void completeSelectedWorkOrder() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            workOrderService.completeWorkOrder(selected);
            refreshData();

            AlertHelper.showInfo(lang.get(
                    "workOrder.completed"),
                    lang.get("workOrder.completedMsg"));

        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
        }
    }
}
