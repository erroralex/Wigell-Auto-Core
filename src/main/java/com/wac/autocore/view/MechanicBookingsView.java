package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.LanguageManager;
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
    private static final LanguageManager lang =  LanguageManager.getInstance();

    public MechanicBookingsView(Mechanic mechanic, Runnable onClose) {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setAlignment(Pos.TOP_LEFT);

        nameLabel.getStyleClass().add("text-content");

        titleLabel = new Label(lang.get("mechanic.bookingsTitle"));
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
        Button close = new Button(lang.get("btn.close"));
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

        TableColumn<Booking, String> dateColumn = new TableColumn<>(lang.get("table.date"));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate().toString()));

        TableColumn<Booking, String> vehicleColumn = new TableColumn<>(lang.get("table.vehicle"));
        vehicleColumn.setCellValueFactory(cellData -> {
            Vehicle v = Database.getVehicleById(cellData.getValue().getVehicleId());
            String label = (v != null) ? v.getBrand() + " " + v.getModel() : lang.get("table.unknown");
            return new SimpleStringProperty(label);
        });

        TableColumn<Booking, String> descColumn = new TableColumn<>(lang.get("table.desc"));
        descColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));

        TableColumn<Booking, String> statusColumn = new TableColumn<>(lang.get("table.status"));
        statusColumn.setCellValueFactory(cellData ->
                        new SimpleStringProperty(lang.get("booking.status." + cellData.getValue().getStatus())));

        bookingTableView.getColumns().add(dateColumn);
        bookingTableView.getColumns().add(vehicleColumn);
        bookingTableView.getColumns().add(descColumn);
        bookingTableView.getColumns().add(statusColumn);

        bookingTableView.setItems(bookingObservableList);
        getChildren().add(bookingTableView);
    }
}
