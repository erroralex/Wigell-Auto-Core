package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Customer;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import java.util.regex.Pattern;

/**
 * <b>CreateCustomerDialog</b>
 * <p>Ansvar: Dialog för att skapa en ny kund.</p>
 */
public class CreateCustomerDialog {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final LanguageManager lang = LanguageManager.getInstance();

    private final GarageSystem garageSystem = new GarageSystem();
    private final Dialog<Boolean> dialog = new Dialog<>();
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final CheckBox vipCheckBox = new CheckBox(lang.get("customer.vip"));
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);

    public CreateCustomerDialog() {
        dialog.setTitle(lang.get("customer.new"));
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, saveButtonType);
        dialog.getDialogPane().setContent(createContent());
        DialogUtil.applyTheme(dialog);
        configureFields();
        configureButtons();
        dialog.setResultConverter(buttonType -> buttonType == saveButtonType);
    }

    public boolean showAndWait() {
        return dialog.showAndWait().orElse(false);
    }

    private void configureFields() {
        nameField.setPromptText(lang.get("table.name"));
        phoneField.setPromptText(lang.get("table.phone"));
        emailField.setPromptText(lang.get("table.email"));

        nameField.getStyleClass().add("input");
        phoneField.getStyleClass().add("input");
        emailField.getStyleClass().add("input");
    }

    private void configureButtons() {
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);

        saveButton.getStyleClass().addAll("btn", "btn-primary");
        cancelButton.getStyleClass().addAll("btn", "btn-secondary");

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!saveCustomer()) {
                event.consume();
            }
        });
    }

    private GridPane createContent() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));

        grid.add(new Label(lang.get("table.name")), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(lang.get("table.phone")), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(lang.get("table.email")), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(vipCheckBox, 1, 3);

        return grid;
    }

    private boolean saveCustomer() {
        resetFieldErrorState();

        String name = getTrimmedValue(nameField);
        String phone = getTrimmedValue(phoneField);
        String email = getTrimmedValue(emailField);

        if (name.isEmpty()) {
            markFieldError(nameField);
            AlertHelper.showError(lang.get("error.validation"), lang.get("error.requiredField", lang.get("table.name")));
            return false;
        }

        if (phone.isEmpty()) {
            markFieldError(phoneField);
            AlertHelper.showError(lang.get("error.validation"), lang.get("error.requiredField", lang.get("table.phone")));
            return false;
        }

        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            markFieldError(emailField);
            AlertHelper.showError(lang.get("error.validation"), lang.get("error.invalidEmail"));
            return false;
        }

        Customer customer = garageSystem.createCustomer(name, phone, email);
        if (customer == null) {
            AlertHelper.showError(lang.get("error.customer"), lang.get("error.customerSave"));
            return false;
        }

        if (vipCheckBox.isSelected()) {
            customer.setVip(true);
        }

        return true;
    }

    private String getTrimmedValue(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void resetFieldErrorState() {
        nameField.getStyleClass().remove("input-error");
        phoneField.getStyleClass().remove("input-error");
        emailField.getStyleClass().remove("input-error");
    }

    private void markFieldError(TextField field) {
        if (!field.getStyleClass().contains("input-error")) {
            field.getStyleClass().add("input-error");
        }
    }

}