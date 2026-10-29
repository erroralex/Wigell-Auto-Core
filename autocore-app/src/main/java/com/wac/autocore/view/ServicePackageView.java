package com.wac.autocore.view;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.dialog.ServicePackageDialog;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>ServicePackageView</b>
 * <p>Responsibility: Admin view for listing, creating, editing and deleting service packages.</p>
 * <p>The total price is the sum of today's catalog prices. The package is only a template; a booking
 * freezes its own prices, so what is shown here never affects existing bookings.</p>
 */
public class ServicePackageView extends BaseView {

    private final ServicePackageService servicePackageService;
    private final ServiceItemService serviceItemService;

    private final TableView<ServicePackage> table = new TableView<>();
    private final ObservableList<ServicePackage> packages = FXCollections.observableArrayList();

    public ServicePackageView(ServicePackageService servicePackageService, ServiceItemService serviceItemService) {
        this.servicePackageService = servicePackageService;
        this.serviceItemService = serviceItemService;

        initializeTable();
        refresh();
        initView();
    }

    @Override
    protected String getTitleKey() {
        return "servicePackage.title";
    }

    @Override
    protected void buildContent() {
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(createButtonBar(), table);
    }

    private void initializeTable() {
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(lang.get("table.empty")));

        TableColumn<ServicePackage, String> nameColumn = new TableColumn<>(lang.get("table.name"));
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getName()));

        TableColumn<ServicePackage, String> descColumn = new TableColumn<>(lang.get("table.desc"));
        descColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDescription()));

        TableColumn<ServicePackage, Integer> countColumn = new TableColumn<>(lang.get("table.serviceCount"));
        countColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getServices().size()));

        TableColumn<ServicePackage, Money> priceColumn = new TableColumn<>(lang.get("table.totalPrice"));
        priceColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(totalPrice(cell.getValue())));

        DialogUtil.configureMoneyColumn(priceColumn);

        table.getColumns().add(nameColumn);
        table.getColumns().add(descColumn);
        table.getColumns().add(countColumn);
        table.getColumns().add(priceColumn);

        table.setItems(packages);
    }

    private static Money totalPrice(ServicePackage servicePackage) {
        Money sum = Money.ZERO;
        for (ServiceItem service : servicePackage.getServices()) {
            sum = sum.add(service.getPrice());
        }
        return sum;
    }

    // Sorted here in the view, so ServicePackageService does not need to change
    private void refresh() {
        List<ServicePackage> all = new ArrayList<>(servicePackageService.findAll());
        all.sort(Comparator.comparing(ServicePackage::getName, String.CASE_INSENSITIVE_ORDER));
        packages.setAll(all);
    }

    private HBox createButtonBar() {
        Button createButton = new Button(lang.get("btn.createNew"));
        Button editButton = new Button(lang.get("btn.edit"));
        Button deleteButton = new Button(lang.get("btn.remove"));

        createButton.getStyleClass().addAll("btn", "btn-primary");
        editButton.getStyleClass().addAll("btn", "btn-secondary");
        deleteButton.getStyleClass().addAll("btn", "btn-secondary");

        // Edit and Delete require a selected row
        BooleanBinding noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        createButton.setOnAction(event -> openDialog(
                ServicePackageDialog.forCreate(servicePackageService, serviceItemService.listAll())));
        editButton.setOnAction(event -> openDialog(
                ServicePackageDialog.forEdit(servicePackageService, serviceItemService.listAll(), selectedPackage())));
        deleteButton.setOnAction(event -> deleteSelected());

        HBox toolbar = new HBox(createButton, editButton, deleteButton);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private ServicePackage selectedPackage() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void openDialog(ServicePackageDialog dialog) {
        dialog.showAndWait().ifPresent(saved -> refresh());
    }

    private void deleteSelected() {
        ServicePackage selected = selectedPackage();
        if (selected == null) {
            return;
        }

        boolean confirmed = AlertHelper.showConfirmation(
                lang.get("servicePackage.deleteConfirmTitle"),
                lang.get("servicePackage.deleteConfirmMsg", selected.getName()));
        if (!confirmed) {
            return;
        }

        try {
            servicePackageService.delete(selected.getId());
            refresh();
        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
        }
    }
}
