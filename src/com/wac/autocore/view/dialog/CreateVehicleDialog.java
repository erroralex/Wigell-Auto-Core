package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/**
 * <b>CreateVehicleDialog</b>
 * <p>Ansvar: Dialog för att skapa ett fordon.</p>
 */
public class CreateVehicleDialog {

    private final GarageSystem garageSystem = new GarageSystem();
    private final Dialog<Boolean> dialog = new Dialog<>();
    private final TextField registrationNumberField = new TextField();
    private final TextField makeField = new TextField();
    private final TextField modelField = new TextField();
    private final TextField yearField = new TextField();
    private final ComboBox<Customer> customerComboBox = new ComboBox<>();
    private final ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    private final ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

    public CreateVehicleDialog() {
        dialog.setTitle("New Vehicle");
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        dialog.getDialogPane().setContent(createContent());
        DialogUtil.applyTheme(dialog.getDialogPane());
        configureFields();
        configureCustomerComboBox();
        configureButtons();
        dialog.setResultConverter(buttonType -> buttonType == saveButtonType);
    }

    public boolean showAndWait() {
        return dialog.showAndWait().orElse(false);
    }

    private void configureFields() {
        registrationNumberField.setPromptText("Registration Number");
        makeField.setPromptText("Make");
        modelField.setPromptText("Model");
        yearField.setPromptText("Year");

        registrationNumberField.getStyleClass().add("input");
        makeField.getStyleClass().add("input");
        modelField.getStyleClass().add("input");
        yearField.getStyleClass().add("input");
        customerComboBox.getStyleClass().add("combo-box");
    }

    private void configureCustomerComboBox() {
        customerComboBox.getItems().setAll(Database.getCustomers());
        customerComboBox.setPromptText("Customer");
        customerComboBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });
    }

    private void configureButtons() {
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);

        saveButton.getStyleClass().addAll("btn", "btn-primary");
        cancelButton.getStyleClass().addAll("btn", "btn-secondary");

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!saveVehicle()) {
                event.consume();
            }
        });
    }

    private GridPane createContent() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));

        grid.add(new Label("Registration Number"), 0, 0);
        grid.add(registrationNumberField, 1, 0);
        grid.add(new Label("Make"), 0, 1);
        grid.add(makeField, 1, 1);
        grid.add(new Label("Model"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("Year"), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label("Customer"), 0, 4);
        grid.add(customerComboBox, 1, 4);

        return grid;
    }

    private boolean saveVehicle() {
        resetFieldErrorState();

        String registrationNumber = getTrimmedValue(registrationNumberField);
        String make = getTrimmedValue(makeField);
        String model = getTrimmedValue(modelField);
        String yearValue = getTrimmedValue(yearField);
        Customer selectedCustomer = customerComboBox.getValue();

        if (registrationNumber.isEmpty()) {
            markFieldError(registrationNumberField);
            AlertHelper.showError("Validation Error", "Registration Number must be provided.");
            return false;
        }

        if (selectedCustomer == null) {
            AlertHelper.showError("Validation Error", "Customer must be selected.");
            return false;
        }

        int year;
        try {
            year = Integer.parseInt(yearValue);
        } catch (NumberFormatException exception) {
            markFieldError(yearField);
            AlertHelper.showError("Validation Error", "Year must be a whole number.");
            return false;
        }

        Vehicle vehicle = garageSystem.createVehicle(
                registrationNumber,
                make,
                model,
                year,
                selectedCustomer.getId()
        );

        if (vehicle == null) {
            AlertHelper.showError("Could not create vehicle", "The vehicle could not be saved.");
            return false;
        }

        return true;
    }

    private String getTrimmedValue(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void resetFieldErrorState() {
        registrationNumberField.getStyleClass().remove("input-error");
        makeField.getStyleClass().remove("input-error");
        modelField.getStyleClass().remove("input-error");
        yearField.getStyleClass().remove("input-error");
        customerComboBox.getStyleClass().remove("input-error");
    }

    private void markFieldError(TextField field) {
        if (!field.getStyleClass().contains("input-error")) {
            field.getStyleClass().add("input-error");
        }
    }

}