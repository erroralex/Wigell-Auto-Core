package com.wac.autocore.view.component;

import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.security.Permission;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.view.dialog.CreateCustomerDialog;
import com.wac.autocore.view.dialog.CreateVehicleDialog;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.StringConverterUtil;
import com.wac.autocore.view.util.ViewPermissionUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <b>CustomerVehiclePicker</b>
 * <p>Responsibility: Choose a customer and then one of the customer's vehicles, or create new ones.
 * Coordinates the choices (<b>Mediator</b>). Takes services because the dialogs for
 * a new customer and a new vehicle need them.</p>
 */
public class CustomerVehiclePicker extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final StringBinding missingSelectionKey;

    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private List<Vehicle> vehicles;

    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final Label customerLabel = new Label(lang.get("workOrder.field.customer"));
    private final Button newCustomerButton = new Button(lang.get("customer.new"));
    private final ComboBox<Vehicle> vehicleCombo = new ComboBox<>();
    private final Label vehicleLabel = new Label(lang.get("workOrder.field.vehicle"));
    private final Button newVehicleButton = new Button(lang.get("vehicle.new"));

    public CustomerVehiclePicker(CustomerService customerService,
                                 VehicleService vehicleService) {
        setSpacing(12);
        this.customerService = customerService;
        this.vehicleService = vehicleService;
        this.vehicles = vehicleService.findAll();

        customerCombo.getItems().addAll(customerService.findAll());
        customerCombo.setPromptText(lang.get("workOrder.field.customer"));
        customerCombo.setMaxWidth(Double.MAX_VALUE);
        customerCombo.setConverter(StringConverterUtil.display(Customer::getName));
        customerCombo.valueProperty().addListener((observable, oldCustomer, newCustomer) -> showVehiclesFor(newCustomer));

        vehicleCombo.setPromptText(lang.get("workOrder.field.vehicle"));
        vehicleCombo.setMaxWidth(Double.MAX_VALUE);
        vehicleCombo.setConverter(StringConverterUtil.display(CustomerVehiclePicker::describeVehicle));
        vehicleCombo.setDisable(true);

        newCustomerButton.getStyleClass().addAll("btn", "btn-secondary");
        newCustomerButton.setOnAction(event -> createCustomer());
        newVehicleButton.getStyleClass().addAll("btn", "btn-secondary");
        newVehicleButton.setOnAction(event -> createVehicle());
        newVehicleButton.setDisable(true);

        ViewPermissionUtil.hideUnless(Permission.CUSTOMER_WRITE, newCustomerButton);
        ViewPermissionUtil.hideUnless(Permission.VEHICLE_WRITE, newVehicleButton);

        HBox customerRow = createRow(customerCombo, newCustomerButton);
        HBox vehicleRow = createRow(vehicleCombo, newVehicleButton);

        getChildren().addAll(customerLabel, customerRow, vehicleLabel, vehicleRow);

        // Translation key for what is missing, or null when a vehicle is selected.
        missingSelectionKey = Bindings.createStringBinding(() -> {
                    if (customerCombo.getValue() == null) {
                        return "workOrder.hint.selectCustomer";
                    }
                    if (vehicleCombo.getItems().isEmpty()) {
                        return "workOrder.hint.noVehicles";
                    }
                    return vehicleCombo.getValue() == null ? "workOrder.hint.selectVehicle" : null;
                },
                customerCombo.valueProperty(),
                vehicleCombo.valueProperty(),
                vehicleCombo.getItems()
        );
    }

    public StringBinding missingSelectionKeyBinding() {
        return missingSelectionKey;
    }

    public Vehicle getSelectedVehicle() {
        return vehicleCombo.getValue();
    }

    public void focusCustomer() {
        customerCombo.requestFocus();
    }

    private static HBox createRow(ComboBox<?> combo, Button button) {
        HBox.setHgrow(combo, Priority.ALWAYS);
        HBox row = new HBox(8, combo, button);
        row.setFillHeight(true);
        return row;
    }

    private void showVehiclesFor(Customer customer) {
        vehicleCombo.getSelectionModel().clearSelection();
        vehicleCombo.getItems().clear();

        if (customer != null) {
            vehicleCombo.getItems().addAll(vehicles.stream()
                    .filter(vehicle -> vehicle.getCustomerId() == customer.getId())
                    .collect(Collectors.toList()));
        }

        vehicleCombo.setDisable(vehicleCombo.getItems().isEmpty());
        newVehicleButton.setDisable(customer == null);
    }

    private void createCustomer() {
        Optional<Customer> created = new CreateCustomerDialog(customerService).showAndWait();
        if (!created.isPresent()) {
            return;
        }

        int createdId = created.get().getId();
        customerCombo.getItems().setAll(customerService.findAll());
        customerCombo.getItems().stream()
                .filter(customer -> customer.getId() == createdId)
                .findFirst()
                .ifPresent(customerCombo::setValue);
    }

    private void createVehicle() {
        Optional<Vehicle> created = new CreateVehicleDialog(
                customerService, vehicleService, customerCombo.getValue()).showAndWait();
        if (!created.isPresent()) {
            return;
        }

        int createdId = created.get().getId();
        vehicles = vehicleService.findAll();
        showVehiclesFor(customerCombo.getValue());
        vehicleCombo.getItems().stream()
                .filter(vehicle -> vehicle.getId() == createdId)
                .findFirst()
                .ifPresent(vehicleCombo::setValue);
    }

    public static String describeVehicle(Vehicle vehicle) {
        return Stream.of(vehicle.getRegistrationNumber(), vehicle.getBrand(), vehicle.getModel())
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.joining(" "));
    }
}
