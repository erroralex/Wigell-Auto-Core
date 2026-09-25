package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


/**
 * <b>CreateBookingDialog</b>
 * <p>Ansvar: Dialog för att skapa en bokning.</p>
 */
public class CreateBookingDialog extends Dialog<CreateBookingDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final BookingService bookingService;

    private final ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final TextField descriptionTextField = new TextField();
    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final ComboBox<ServiceItem> serviceItemComboBox = new ComboBox<>();
    private final Label errorLabel = new Label();
    private final Label estimatedTimeLabel = new Label();
    private final List<Booking> bookingList;

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public CreateBookingDialog(BookingService bookingService) {
        this.bookingService = bookingService;

        this.bookingList = bookingService.listAll();

        errorLabel.getStyleClass().add("text-error");

        setTitle(lang.get("booking.new"));
        setHeaderText(lang.get("booking.create"));

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        vehicleComboBox.getItems().addAll(Database.getVehicles());
        mechanicComboBox.getItems().addAll(Database.getMechanics());
        serviceItemComboBox.getItems().addAll(bookingService.listAllServiceItems());

        setContent();
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                errorLabel,
                new Label(lang.get("table.vehicle")), vehicleComboBox,
                new Label(lang.get("table.date")), datePicker,
                new Label(lang.get("table.desc")), descriptionTextField,
                new Label("Mechanic"), mechanicComboBox,
                new Label("Service"), serviceItemComboBox,
                new Label("Estimated time: "), estimatedTimeLabel
        );
        getDialogPane().setContent(content);

        handleInput();
    }

    private void handleInput() {

        inputEventListeners();

        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {

            if (descriptionTextField.getText().isEmpty()
                    && vehicleComboBox.getValue() == null
                    && datePicker.getValue() == null) {
                errorLabel.setText(lang.get("error.fields"));
                vehicleComboBox.getStyleClass().add("input-error");
                datePicker.getStyleClass().add("input-error");
                descriptionTextField.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (vehicleComboBox.getValue() == null) {
                errorLabel.setText(lang.get("error.vehicleSelect"));
                vehicleComboBox.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (datePicker.getValue() == null) {
                errorLabel.setText(lang.get("error.dateSelect"));
                datePicker.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (descriptionTextField.getText().isEmpty()) {
                errorLabel.setText(lang.get("error.desc"));
                descriptionTextField.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (mechanicComboBox.getValue() == null) {
                errorLabel.setText("Select a mechanic.");
                event.consume();
                return;
            }

            if (serviceItemComboBox.getValue() == null) {
                errorLabel.setText("Select a service.");
                event.consume();
                return;
            }


            LocalDate date = datePicker.getValue();
            int vehicleId = vehicleComboBox.getValue().getId();

            if (bookingService.isVehicleBooked(vehicleId, date)) {
                AlertHelper.showError(
                        lang.get("error.booking"),
                        lang.get("error.vehicleBooked",
                                vehicleComboBox.getValue().getRegistrationNumber())
                );
                event.consume();
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {

                LocalTime startTime = LocalTime.now();
                LocalTime endTime = startTime.plusMinutes(
                        serviceItemComboBox.getValue().getEstimatedMinutes()
                );

                return new Result(
                        vehicleComboBox.getValue().getId(),
                        mechanicComboBox.getValue().getId(),
                        datePicker.getValue(),
                        startTime,
                        endTime,
                        descriptionTextField.getText()

                );
            }
            return null;
        });
    }

    private void inputEventListeners() {
        vehicleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                vehicleComboBox.getStyleClass().removeAll("input-error");
            }
        });

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                datePicker.getStyleClass().removeAll("input-error");
            }
        });

        descriptionTextField.textProperty().addListener((obs, oldVal, newVal) -> {
          if (newVal != null) {
              descriptionTextField.getStyleClass().removeAll("input-error");
          }
        });
    }

    public static class Result {
        private final int vehicleId;
        private final LocalDate date;
        private final String description;
        private final int mechanicId;
        private final LocalTime startTime;
        private final LocalTime endTime;

        public Result(int vehicleId, int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime, String description) {

            this.vehicleId = vehicleId;
            this.mechanicId = mechanicId;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.description = description;
        }

        public int getVehicleId() {
            return vehicleId;
        }

        public int getMechanicId() {
            return mechanicId;
        }

        public LocalDate getDate() {
            return date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public String getDescription() {
            return description;
        }

    }


}
