package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.dialog.CreateVehicleDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>VehicleView</b>
 * <p>Ansvar: Visar och hanterar fordon i användargränssnittet.</p>
 */
public class VehicleView extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final GarageSystem garageSystem;

    // Markörobjekt för "alla kunder". Namnet visas aldrig, texten kommer från converter nedan
    private static final Customer ALL_CUSTOMERS = new Customer(0, "", "", "");

    private final TableView<Vehicle> vehicleTable = new TableView<>();
    private final ObservableList<Vehicle> masterData = FXCollections.observableArrayList();
    private final FilteredList<Vehicle> filteredData = new FilteredList<>(masterData, vehicle -> true);
    private final ObservableList<Customer> customerFilterOptions = FXCollections.observableArrayList();
    private final ComboBox<Customer> customerFilter = new ComboBox<>();
    private final Map<Integer, String> customerNamesById = new HashMap<>();

    public VehicleView(GarageSystem garageSystem) {
        this.garageSystem = garageSystem;
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(vehicleTable, Priority.ALWAYS);

        Label title = new Label(lang.get("vehicle.title"));
        title.getStyleClass().add("text-title");

        loadCustomers();
        loadMasterData();
        initializeTable();
        configureCustomerFilter();

        this.getChildren().addAll(title, createToolbar(), vehicleTable);
    }

    private void loadCustomers() {
        customerNamesById.clear();
        customerFilterOptions.setAll(Database.getCustomers());

        for (Customer customer : Database.getCustomers()) {
            customerNamesById.put(customer.getId(), customer.getName());
        }
    }

    private void loadMasterData() {
        masterData.setAll(Database.getVehicles());
    }

    private void refreshData() {
        loadCustomers();
        customerFilterOptions.add(0, ALL_CUSTOMERS);
        masterData.setAll(Database.getVehicles());
        applyCustomerFilter(customerFilter.getValue());
    }

    private void initializeTable() {
        TableColumn<Vehicle, String> idColumn = new TableColumn<>(lang.get("table.id"));
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        TableColumn<Vehicle, String> registrationNumberColumn = new TableColumn<>(lang.get("table.regNumber"));
        registrationNumberColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRegistrationNumber())
        );

        TableColumn<Vehicle, String> makeColumn = new TableColumn<>(lang.get("table.make"));
        makeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBrand())
        );

        TableColumn<Vehicle, String> modelColumn = new TableColumn<>(lang.get("table.model"));
        modelColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getModel())
        );

        TableColumn<Vehicle, String> yearColumn = new TableColumn<>(lang.get("table.year"));
        yearColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getYear()))
        );

        TableColumn<Vehicle, String> ownerColumn = new TableColumn<>(lang.get("table.owner"));
        ownerColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(resolveOwnerName(cellData.getValue().getCustomerId()))
        );

        vehicleTable.getColumns().addAll(
                idColumn,
                registrationNumberColumn,
                makeColumn,
                modelColumn,
                yearColumn,
                ownerColumn
        );
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vehicleTable.setPlaceholder(new Label(lang.get("table.empty")));
        vehicleTable.setItems(filteredData);
    }

    private void configureCustomerFilter() {
        customerFilter.getStyleClass().add("combo-box");
        customerFilter.setPromptText(lang.get("vehicle.filterPrompt"));
        customerFilter.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                if (customer == null) {
                    return "";
                }
                // Markörobjektet översätts här, eftersom ett static-fält inte kan följa språkbyten
                return customer == ALL_CUSTOMERS ? lang.get("vehicle.allCustomers") : customer.getName();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });

        customerFilterOptions.add(0, ALL_CUSTOMERS);
        customerFilter.setItems(customerFilterOptions);
        customerFilter.setValue(ALL_CUSTOMERS);
        customerFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyCustomerFilter(newValue));
    }

    private void applyCustomerFilter(Customer selectedCustomer) {
        filteredData.setPredicate(vehicle -> {
            if (selectedCustomer == null || selectedCustomer.getId() == ALL_CUSTOMERS.getId()) {
                return true;
            }

            return vehicle.getCustomerId() == selectedCustomer.getId();
        });
    }

    private String resolveOwnerName(int customerId) {
        String customerName = customerNamesById.get(customerId);
        return customerName == null ? "" : customerName;
    }

    private HBox createToolbar() {
        Button newVehicleButton = new Button(lang.get("vehicle.new"));
        newVehicleButton.getStyleClass().addAll("btn", "btn-primary");
        newVehicleButton.setOnAction(event -> openCreateVehicleDialog());

        HBox toolbar = new HBox(15, newVehicleButton, customerFilter);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(customerFilter, Priority.ALWAYS);
        return toolbar;
    }

    private void openCreateVehicleDialog() {
        CreateVehicleDialog dialog = new CreateVehicleDialog();
        if (dialog.showAndWait()) {
            refreshData();
        }
    }
}
