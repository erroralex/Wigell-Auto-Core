package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>BookingView</b>
 * <p>Ansvar: Visar och hanterar bokningar i användargränssnittet.</p>
 */
public class BookingView extends VBox {

    private final ObservableList<Booking> bookingObservableList;

    private final Map<Integer, Vehicle> vehicleMap;

    public BookingView() {
        this.bookingObservableList = FXCollections.observableArrayList(Database.getBookings());

        vehicleMap = Database.getVehicles()
                .stream()
                .collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle));

        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);

        show();
    }

    private void show() {
        renderTitle();
        renderDescText();
        renderTable();
    }

    private void renderTitle() {
        Label title = new Label("BOOKINGS");
        title.setId("h1");
        getChildren().add(title);
        title.getStyleClass().add("text-title");
    }

    private void renderDescText() {
        Label description = new Label("Bookings are shown in a descending order based on date (Newest to Oldest)");
        description.getStyleClass().add("text-secondary");
        getChildren().add(description);
    }

    private void renderTable() {
        TableView<Booking> bookingTableView = new TableView<>();

        bookingTableView.setEditable(false);
        bookingTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(bookingTableView, Priority.ALWAYS);

        TableColumn<Booking, Number> bookingIdColumn = new TableColumn<>("Booking ID");
        TableColumn<Booking, String> regIdColumn = new TableColumn<>("Registration");
        TableColumn<Booking, LocalDate> dateColumn = new TableColumn<>("Date");
        TableColumn<Booking, String> descriptionColumn = new TableColumn<>("Description");
        TableColumn<Booking, String> statusColumn = new TableColumn<>("Status");

        bookingIdColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getId()));

        regIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(fetchRegId(cellData.getValue().getVehicleId())));

        dateColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDate()));
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);

        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        bookingTableView.getColumns().add(bookingIdColumn);
        bookingTableView.getColumns().add(regIdColumn);
        bookingTableView.getColumns().add(dateColumn);
        bookingTableView.getColumns().add(descriptionColumn);
        bookingTableView.getColumns().add(statusColumn);

        bookingTableView.setItems(bookingObservableList);
        bookingTableView.getSortOrder().add(dateColumn);

        getChildren().add(bookingTableView);
    }

    private String fetchRegId(int vehicleId) {
        
        Vehicle vehicle = vehicleMap.get(vehicleId);

        if (vehicle != null) {
            return vehicle.getRegistrationNumber();
        }

        return "NOT FOUND";
    }


}
