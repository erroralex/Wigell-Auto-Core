package com.wac.autocore.view;


import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.MechanicService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MechanicBookingsView extends BaseView {

    private final MechanicService mechanicService;
    private final ObservableList<Booking> bookingObservableList = FXCollections.observableArrayList();
    private final TableView<Booking> bookingTableView = new TableView<>();
    private final Label nameLabel = new Label();
    private final Runnable onClose;

    public MechanicBookingsView(Mechanic mechanic, MechanicService mechanicService, Runnable onClose) {
        this.mechanicService = mechanicService;
        this.onClose = onClose;

        initializeTable();
        setMechanic(mechanic);

        initView();
    }

    @Override
    protected void buildHeader() {
        title = new Label(lang.get(getTitleKey()));
        title.getStyleClass().add("text-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, title, spacer, closeButton(onClose));
        header.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(header);
    }

    @Override
    protected String getTitleKey() {
        return "mechanic.bookingsTitle";
    }

    @Override
    protected void buildContent() {
        getChildren().addAll(nameLabel, bookingTableView);
    }

    private Button closeButton(Runnable onClose) {
        Button close = new Button(lang.get("btn.close"));
        close.getStyleClass().add("btn-secondary");
        close.setOnAction(e -> onClose.run());
        return close;
    }

    public void setMechanic(Mechanic mechanic) {
        nameLabel.setText(mechanic.getName());
        bookingObservableList.setAll(mechanicService.listBookingsByMechanicId(mechanic.getId()));
    }

    private void initializeTable() {
        bookingTableView.setEditable(false);
        bookingTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bookingTableView.setPlaceholder(new Label(lang.get("table.empty")));
        VBox.setVgrow(bookingTableView, Priority.ALWAYS);

        TableColumn<Booking, String> dateColumn = new TableColumn<>(lang.get("table.date"));
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate().toString()));

        TableColumn<Booking, String> vehicleColumn = new TableColumn<>(lang.get("table.vehicle"));
        vehicleColumn.setCellValueFactory(cellData -> {
            Vehicle v = mechanicService.getVehicleById(cellData.getValue().getVehicleId()).orElse(null);
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
    }
}
