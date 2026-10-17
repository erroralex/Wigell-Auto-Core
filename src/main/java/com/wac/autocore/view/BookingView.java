package com.wac.autocore.view;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private final Button btnCancelBooking = new Button(lang.get("btn.cancelBooking"));
    private final CheckBox showCancelled = new CheckBox(lang.get("booking.showCancelled"));

    public BookingView(BookingService bookingService, ServicePackageService servicePackageService) {
        this.bookingService = bookingService;
        this.servicePackageService = servicePackageService;
        this.bookingObservableList = FXCollections.observableArrayList(bookingService.listActive());

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
        TableColumn<Booking, String> mechanicColumn =       new TableColumn<>(lang.get("table.suggestedMechanic"));
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
            btnCancelBooking.setDisable(none || newVal.getStatus() != BookingStatus.BOOKED);
                }
        );
    }

    // Avbokade bokningar döljs tills kryssrutan markeras. Filtret ligger i servicen, vyn väljer bara vilken lista den visar.
    private void refreshBookings() {
        bookingObservableList.setAll(showCancelled.isSelected()
                ? bookingService.listAll()
                : bookingService.listActive());
    }

    // Förslaget är valfritt, så en bokning utan förslag visar ett tomt fält.
    private String fetchMechanic(Integer mechanicId) {
        if (mechanicId == null) {
            return "";
        }

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

        btnCancelBooking.getStyleClass().addAll("btn", btnPrimary);
        btnCancelBooking.setOnAction(e -> cancelSelectedBooking());
        btnCancelBooking.setDisable(true);

        showCancelled.setOnAction(e -> refreshBookings());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox hBox = new HBox(15, btnCreate, btnEditServices, btnCopyBooking, btnCancelBooking, spacer, showCancelled);
        hBox.setPadding(new Insets(15, 0, 0, 0));
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    private void cancelSelectedBooking() {
        Booking selected = bookingTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        boolean confirmed = AlertHelper.showWarningChoice(
                lang.get("booking.cancelConfirmTitle"),
                lang.get("booking.cancelConfirmMsg", selected.getId()),
                lang.get("btn.cancelBooking"),
                lang.get("btn.keep"));

        if (!confirmed) {
            return;
        }

        try {
            bookingService.cancel(selected.getId());
            refreshBookings();
            AlertHelper.showInfo(lang.get("booking.cancelled"), lang.get("booking.cancelledMsg"));
        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
        }
    }

    private void openCreateBookingDialog() {
        CreateBookingDialog dialog = new CreateBookingDialog(bookingService, servicePackageService);

        dialog.showAndWait().ifPresent(result -> {
            try {
                bookingService.create(
                    result.getVehicleId(),
                    result.getMechanicId(),
                    result.getDate(),
                    result.getStartTime(),
                    result.getDescription(),
                    result.getServiceItemIds()
                );

                refreshBookings();
                AlertHelper.showInfo(lang.get("booking.created"), lang.get("booking.createdMsg"));
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
            }
        });
    }

    private void openCopyBookingDialog() {
        Booking selected = bookingTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        CreateBookingDialog dialog = new CreateBookingDialog(bookingService, servicePackageService, selected);

        dialog.showAndWait().ifPresent(result -> {
            try {
                // createFromPrevious tolkar mechanicId == null som "ärv förslaget". Har användaren rensat förslaget
                // skapas bokningen i stället med create, som ger samma resultat (dagens priser) men utan förslag.
                if (result.getMechanicId() == null) {
                    bookingService.create(
                            result.getVehicleId(),
                            null,
                            result.getDate(),
                            result.getStartTime(),
                            result.getDescription(),
                            result.getServiceItemIds()
                    );
                } else {
                    bookingService.createFromPrevious(
                            selected.getId(),
                            result.getDate(),
                            result.getStartTime(),
                            result.getMechanicId(),
                            result.getVehicleId(),
                            result.getDescription(),
                            result.getServiceItemIds()
                    );
                }

                refreshBookings();
                AlertHelper.showInfo(lang.get("booking.created"), lang.get("booking.createdMsg"));
            } catch(RuntimeException e) {
                ErrorFacade.handle(e);
            }
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

            try {

                bookingService.update(
                        selected.getId(),
                        selected.getVehicleId(),
                        selected.getMechanicId(),
                        selected.getDate(),
                        selected.getStartTime(),
                        selected.getDescription(),
                        result.getServiceItemIds()
                );

                refreshBookings();
                AlertHelper.showInfo(lang.get("booking.updated"), lang.get("booking.updatedMsg"));
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
            }
        });
    }
}
