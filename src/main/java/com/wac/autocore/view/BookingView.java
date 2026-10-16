package com.wac.autocore.view;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.dialog.CreateBookingDialog;
import com.wac.autocore.view.dialog.EditBookingServicesDialog;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <b>BookingView</b>
 * <p>Ansvar: Visar och hanterar bokningar i användargränssnittet.</p>
 */
public class BookingView extends BaseView {

    private final BookingService bookingService;
    private final ServicePackageService servicePackageService;

    private final ObservableList<Booking> bookingObservableList;

    private TableView<Booking> bookingTableView;

    private final Map<Integer, Vehicle> vehicleMap;

    private final Map<Integer, Mechanic> mechanicMap;

    private final Button btnCreate = new Button(lang.get("btn.createNew"));
    private final Button btnEditServices = new Button(lang.get("btn.edit"));
    private final Button btnCopyBooking = new Button(lang.get("btn.newBookingFromThis"));

    public BookingView(BookingService bookingService, ServicePackageService servicePackageService) {
        this.bookingService = bookingService;
        this.servicePackageService = servicePackageService;
        this.bookingObservableList = FXCollections.observableArrayList(bookingService.listAll());

        vehicleMap = bookingService.listAllVehicles()
                .stream()
                .collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle));
        mechanicMap = bookingService.listAllMechanics()
                .stream()
                .collect(Collectors.toMap(Mechanic::getId, mechanic -> mechanic));

        initializeTable();

        initView();


    }

    @Override
    protected String getTitleKey() {
        return "booking.title";
    }

    @Override
    protected void buildContent() {
        VBox.setVgrow(bookingTableView, Priority.ALWAYS);
        getChildren().addAll(createButtonBar(), bookingTableView);
    }

    private void initializeTable() {
        bookingTableView = new TableView<>();

        bookingTableView.setEditable(false);
        bookingTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Booking, Number> bookingIdColumn =      new TableColumn<>(lang.get("table.bookingId"));
        TableColumn<Booking, String> regIdColumn =          new TableColumn<>(lang.get("table.regNumber"));
        TableColumn<Booking, LocalDate> dateColumn =        new TableColumn<>(lang.get("table.date"));
        TableColumn<Booking, String> descriptionColumn =    new TableColumn<>(lang.get("table.desc"));
        TableColumn<Booking, String> statusColumn =         new TableColumn<>(lang.get("table.status"));
        TableColumn<Booking, String> mechanicColumn =       new TableColumn<>(lang.get("table.mechanic"));
        TableColumn<Booking, LocalTime> startTimeColumn =   new TableColumn<>(lang.get("table.startTime"));
        TableColumn<Booking, LocalTime> endTimeColumn =     new TableColumn<>(lang.get("table.endTime"));

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
                new SimpleStringProperty(lang.get("booking.status." + cellData.getValue().getStatus())));

        mechanicColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(fetchMechanic(cellData.getValue().getMechanicId())));

        startTimeColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getStartTime()));

        endTimeColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getEndTime()));

        bookingTableView.getColumns().add(bookingIdColumn);
        bookingTableView.getColumns().add(regIdColumn);
        bookingTableView.getColumns().add(dateColumn);
        bookingTableView.getColumns().add(descriptionColumn);
        bookingTableView.getColumns().add(statusColumn);
        bookingTableView.getColumns().add(mechanicColumn);
        bookingTableView.getColumns().add(startTimeColumn);
        bookingTableView.getColumns().add(endTimeColumn);

        bookingTableView.setPlaceholder(new Label(lang.get("table.empty")));
        bookingTableView.setItems(bookingObservableList);
        bookingTableView.getSortOrder().add(dateColumn);

        bookingTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
            boolean none = newVal == null;
            btnEditServices.setDisable(none);
            btnCopyBooking.setDisable(none);
                }
        );
    }

    private String fetchMechanic(int mechanicId) {
        Mechanic mechanic = mechanicMap.get(mechanicId);

        if (mechanic != null) {
            return mechanic.getName();
        }

        return lang.get("table.notFound");
    }

    private String fetchRegId(int vehicleId) {

        Vehicle vehicle = vehicleMap.get(vehicleId);

        if (vehicle != null) {
            return vehicle.getRegistrationNumber();
        }

        return lang.get("table.notFound");
    }

    private HBox createButtonBar() {
        String btnPrimary = "btn-primary";

        btnCreate.getStyleClass().addAll("btn", btnPrimary);
        btnCreate.setOnAction(event -> openCreateBookingDialog());

        btnEditServices.getStyleClass().addAll("btn", btnPrimary);
        btnEditServices.setOnAction(event -> openEditServicesDialog());
        btnEditServices.setDisable(true);

        btnCopyBooking.getStyleClass().addAll("btn", btnPrimary);
        btnCopyBooking.setOnAction(e -> openCopyBookingDialog());
        btnCopyBooking.setDisable(true);

        HBox hBox = new HBox(15, btnCreate, btnEditServices, btnCopyBooking);
        hBox.setPadding(new Insets(15, 0, 0, 0));
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    private void openCreateBookingDialog() {
        CreateBookingDialog dialog = new CreateBookingDialog(bookingService, servicePackageService);

        dialog.showAndWait().ifPresent(result -> {
                bookingObservableList.setAll(bookingService.listAll());
                AlertHelper.showInfo(lang.get("booking.created"), lang.get("booking.createdMsg"));
        });
    }

    private void openCopyBookingDialog() {
        Booking selected = bookingTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        CreateBookingDialog dialog = new CreateBookingDialog(bookingService, servicePackageService, selected);

        dialog.showAndWait().ifPresent(result -> {
            bookingObservableList.setAll(bookingService.listAll());
            AlertHelper.showInfo(lang.get("booking.created"), lang.get("booking.createdMsg"));
        });
    }

    private void openEditServicesDialog() {
        Booking selected = bookingTableView.getSelectionModel().getSelectedItem();

        if (selected == null)
            return;

        if (!selected.getStatus().isEditable()) {
            AlertHelper.showError(
                    lang.get("error.title"),
                    lang.get("error.bookingStarted")
            );
            return;
        }

        EditBookingServicesDialog dialog = new EditBookingServicesDialog(selected, bookingService);

        dialog.showAndWait().ifPresent(result -> {

            bookingObservableList.setAll(bookingService.listAll());
            AlertHelper.showInfo(
                    lang.get("booking.updated"),
                    lang.get("booking.updatedMsg")
            );
        });
    }
}
