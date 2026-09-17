package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.List;


/**
 * <b>CreateBookingDialog</b>
 * <p>Ansvar: Dialog för att skapa en bokning.</p>
 */
public class CreateBookingDialog extends Dialog<CreateBookingDialog.Result> {

    private final ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final TextField descriptionTextField = new TextField();
    private final Label errorLabel = new Label();
    private final List<Booking> bookingList = Database.getBookings();

    private final ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

    public CreateBookingDialog() {

        errorLabel.getStyleClass().add("text-error");

        setTitle("New Booking");
        setHeaderText("Create Booking");

        DialogUtil.applyTheme(getDialogPane());

        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        vehicleComboBox.getItems().addAll(Database.getVehicles());

        setContent();
    }

    private boolean isVehicleBooked(int vehicleId, LocalDate date) {
        for (Booking booking : bookingList) {
            if (booking.getVehicleId() == vehicleId && booking.getDate().equals(date)) {
                return true;
            }
        }
        return false;
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                errorLabel,
                new Label("Vehicle"), vehicleComboBox,
                new Label("Date"), datePicker,
                new Label("Description"), descriptionTextField
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
                errorLabel.setText("Please fill in the required fields.");
                vehicleComboBox.getStyleClass().add("input-error");
                datePicker.getStyleClass().add("input-error");
                descriptionTextField.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (vehicleComboBox.getValue() == null) {
                errorLabel.setText("Please select a vehicle.");
                vehicleComboBox.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (datePicker.getValue() == null) {
                errorLabel.setText("Please select a date.");
                datePicker.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (descriptionTextField.getText().isEmpty()) {
                errorLabel.setText("Please provide a description for the booking.");
                descriptionTextField.getStyleClass().add("input-error");
                event.consume();
                return;
            }


            LocalDate date = datePicker.getValue();
            int vehicleId = vehicleComboBox.getValue().getId();

            if (isVehicleBooked(vehicleId, date)) {
                    AlertHelper.showError("Could not complete booking",
                            "Vehicle: " + vehicleComboBox.getValue()
                                    + "is already booked at this date."
                    );
                    event.consume();
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new Result(
                        vehicleComboBox.getValue().getId(),
                        datePicker.getValue(),
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

        public Result(int vehicleId, LocalDate date, String description) {

            this.vehicleId = vehicleId;
            this.date = date;
            this.description = description;
        }

        public int getVehicleId() {
            return vehicleId;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDescription() {
            return description;
        }
    }


}
