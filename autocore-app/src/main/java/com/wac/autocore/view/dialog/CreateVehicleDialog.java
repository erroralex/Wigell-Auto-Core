package com.wac.autocore.view.dialog;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/**
 * <b>CreateVehicleDialog</b>
 * <p>Responsibility: Dialog for creating a vehicle.</p>
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class CreateVehicleDialog extends FormDialog<Vehicle> {

    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final TextField registrationNumberField = new TextField();
    private final TextField makeField = new TextField();
    private final TextField modelField = new TextField();
    private final TextField yearField = new TextField();
    private final ComboBox<Customer> customerComboBox = new ComboBox<>();

    private final Customer preselectedCustomer;
    private Vehicle createdVehicle;

    public CreateVehicleDialog(CustomerService customerService, VehicleService vehicleService) {
        this(customerService, vehicleService, null);
    }

    // With a preselected customer, e.g. when the vehicle is created from another dialog.
    public CreateVehicleDialog(CustomerService customerService, VehicleService vehicleService, Customer preselectedCustomer) {
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        this.preselectedCustomer = preselectedCustomer;

        configureFields();
        configureCustomerComboBox();
        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "vehicle.new";
    }

    @Override
    protected Node buildContent() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");

        grid.add(new Label(lang.get("table.regNumber")), 0, 0);
        grid.add(registrationNumberField, 1, 0);
        grid.add(new Label(lang.get("table.make")), 0, 1);
        grid.add(makeField, 1, 1);
        grid.add(new Label(lang.get("table.model")), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label(lang.get("table.year")), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label(lang.get("table.customer")), 0, 4);
        grid.add(customerComboBox, 1, 4);

        return grid;
    }

    @Override
    protected void submit() {
        resetFieldErrorState();

        String registrationNumber = getTrimmedValue(registrationNumberField);
        String make = getTrimmedValue(makeField);
        String model = getTrimmedValue(modelField);
        String yearValue = getTrimmedValue(yearField);
        Customer selectedCustomer = customerComboBox.getValue();

        Integer year = null;
        if (!yearValue.isEmpty()) {
            try {
                year = Integer.valueOf(yearValue);
            } catch (NumberFormatException exception) {
                markFieldError(yearField);
                throw new ValidationException("error.invalidYear");
            }
        }

        createdVehicle = vehicleService.create(
                registrationNumber,
                make,
                model,
                year,
                selectedCustomer == null ? null : selectedCustomer.getId()
        );
    }

    @Override
    protected Vehicle buildResult() {
        return createdVehicle;
    }

    private String getTrimmedValue(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void configureFields() {
        registrationNumberField.setPromptText(lang.get("table.regNumber"));
        makeField.setPromptText(lang.get("table.make"));
        modelField.setPromptText(lang.get("table.model"));
        yearField.setPromptText(lang.get("table.year"));

        registrationNumberField.getStyleClass().add("input");
        makeField.getStyleClass().add("input");
        modelField.getStyleClass().add("input");
        yearField.getStyleClass().add("input");
        customerComboBox.getStyleClass().add("combo-box");
    }

    private void configureCustomerComboBox() {
        customerComboBox.getItems().setAll(customerService.findAll());
        customerComboBox.setPromptText(lang.get("table.customer"));
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

        if (preselectedCustomer != null) {
            customerComboBox.getItems().stream()
                    .filter(customer -> customer.getId() == preselectedCustomer.getId())
                    .findFirst()
                    .ifPresent(customerComboBox::setValue);
        }
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
