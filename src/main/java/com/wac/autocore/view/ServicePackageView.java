package com.wac.autocore.view;

import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.ServicePackageService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * <b>ServicePackageView</b>
 * <p>Ansvar: Admin-vy för att lista, skapa, redigera och ta bort servicepaket.</p>
 * <p>Totalpriset är summan av dagens katalogpriser. Paketet är bara en mall; en bokning
 * fryser sina egna priser, så det som visas här påverkar aldrig befintliga bokningar.</p>
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
}
