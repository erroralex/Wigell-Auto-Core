package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MechanicBookingsView extends VBox {

    private final ObservableList<Booking> bookingObservableList = FXCollections.observableArrayList();
    private final TableView<Booking> bookingTableView = new TableView<>();
    private final Label nameLabel = new Label();
    private Label titleLabel;

    public MechanicBookingsView(Mechanic mechanic, Runnable onClose) {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setAlignment(Pos.TOP_LEFT);

        nameLabel.getStyleClass().add("text-content");

        titleLabel = new Label("Mechanic Booking Details");
        titleLabel.getStyleClass().add("text-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLabel, spacer, closeButton(onClose));
        header.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(header, nameLabel);

        renderTable();
        setMechanic(mechanic);
    }

    private Button closeButton(Runnable onClose) {
        Button close = new Button("✕");
        close.getStyleClass().add("btn-secondary");
        close.setOnAction(e -> onClose.run());
        return close;
    }

    public void setMechanic(Mechanic mechanic) {
        nameLabel.setText(mechanic.getName());
        bookingObservableList.setAll(Database.getBookingsForMechanic(mechanic.getId()));
    }

    private void renderTable() {
        bookingTableView.setEditable(false);
        bookingTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(bookingTableView, Priority.ALWAYS);

        TableColumn<Booking, String> dateColumn = new TableColumn<>("Date"); // ALEXANDER TODO: ENG TO SWE LANGUAGE SWITCH
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate().toString()));

        TableColumn<Booking, String> vehicleColumn = new TableColumn<>("Vehicle"); // ALEXANDER TODO: ENG TO SWE LANGUAGE SWITCH
        vehicleColumn.setCellValueFactory(cellData -> {
            Vehicle v = Database.getVehicleById(cellData.getValue().getVehicleId());
            String label = (v != null) ? v.getBrand() + " " + v.getModel() : "Unknown";
            return new SimpleStringProperty(label);
        });

        TableColumn<Booking, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));

        TableColumn<Booking, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        bookingTableView.getColumns().add(dateColumn);
        bookingTableView.getColumns().add(vehicleColumn);
        bookingTableView.getColumns().add(descColumn);
        bookingTableView.getColumns().add(statusColumn);

        bookingTableView.setItems(bookingObservableList);
        getChildren().add(bookingTableView);
    }
}
