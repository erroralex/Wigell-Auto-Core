package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.view.util.AlertHelper;
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

/**
 * <b>WorkOrderView</b>
 * <p>Ansvar: Visar och hanterar arbetsordrar i användargränssnittet.</p>
 */
public class WorkOrderView extends VBox {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final TableView<WorkOrder> workOrderTable =  new TableView<>();
    private final ObservableList<WorkOrder> masterData = FXCollections.observableArrayList();

    private final GarageSystem garageSystem = new GarageSystem();

    private final Button btnStart = new Button("Start");
    private final Button btnComplete = new Button("Complete");

    public WorkOrderView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(workOrderTable, Priority.ALWAYS);

        Label title = new Label("Work Orders");
        title.getStyleClass().add("text-title");

        loadMasterData();
        initializeTable();

        HBox buttonBar = createButtonBar();

        workOrderTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateButtonStates(newValue)
        );

        updateButtonStates(null);
        this.getChildren().addAll(title, buttonBar, workOrderTable);
    }

    private void loadMasterData() {
        masterData.setAll(Database.getWorkOrders());
    }

    private void refreshData() {
        masterData.setAll(Database.getWorkOrders());
    }

    private void initializeTable() {

        // -------------------------------------------------------------------------------------------------------------

        TableColumn<WorkOrder, String> idCol = new TableColumn<>("Order-ID");
        idCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId()))
        );

        TableColumn<WorkOrder, String> bookingCol = new TableColumn<>("Booking-ID");
        bookingCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getBookingId()))
        );

        TableColumn<WorkOrder, String> mechanicCol = new TableColumn<>("Mechanic-ID");
        mechanicCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getMechanicId()))
        );

        TableColumn<WorkOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getStatus()))
        );

        // -------------------------------------------------------------------------------------------------------------

        workOrderTable.getColumns().addAll(idCol, bookingCol, mechanicCol, statusCol);
        workOrderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        workOrderTable.setItems(masterData);
    }

    private HBox createButtonBar() {
        btnStart.getStyleClass().addAll("btn", "btn-primary");
        btnStart.setOnAction(event -> startSelectedWorkOrder());

        btnComplete.getStyleClass().addAll("btn", "btn-primary");
        btnComplete.setOnAction(event -> completeSelectedWorkOrder());

        HBox box = new HBox(15, btnStart, btnComplete);
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
        String status = selected.getStatus();
        btnStart.setDisable(!STATUS_CREATED.equals(status));
        btnComplete.setDisable(!STATUS_COMPLETED.equals(status));
    }

    private void startSelectedWorkOrder() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String statusBefore = selected.getStatus();
        garageSystem.startWorkOrder(selected.getId());
        String statusAfter = selected.getStatus();

        if (!statusBefore.equals(statusAfter) && STATUS_IN_PROGRESS.equals(statusAfter)) {
            refreshData();
            AlertHelper.showInfo(
                    "Work order started",
                    "The work order has been started");
        } else  {
            AlertHelper.showError(
                    "Could not start",
                    "The work order could not be started. Check availability of the mechanic");
        }
    }

    private void completeSelectedWorkOrder() {
        WorkOrder selected = workOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        String statusBefore = selected.getStatus();
        garageSystem.completeWorkOrder(selected.getId());
        String statusAfter = selected.getStatus();

        if (!statusBefore.equals(statusAfter) && STATUS_COMPLETED.equals(statusAfter)) {
            refreshData();
            AlertHelper.showInfo(
                    "Workorder completed",
                    "The work order has been completed and is ready for invoice");
        }  else  {
            AlertHelper.showError(
                    "Could not complete",
                    "The work order could not be completed");
        }
    }
}
