package com.wac.autocore.view.component;

import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.view.i18n.LanguageManager;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Objects;

/**
 * ServicePackageSelectorBox
 * Ansvar: Återanvändbar komponent för att välja ett servicepaket.
 * Komponenten hämtar ingen data själv: den som skapar den skickar in tillgängliga paket,
 * och läser resultatet med {@link #getSelectedPackage()} eller
 * {@link #selectedPackageProperty()}.
 * När ett paket väljs ansvarar anroparen (dialogen) för att fylla på sin ServiceSelectorBox.
 */
public class ServicePackageSelectorBox extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final ComboBox<ServicePackage> packageComboBox = new ComboBox<>();
    private final ReadOnlyObjectWrapper<ServicePackage> selectedPackage =
            new ReadOnlyObjectWrapper<>(this, "selectedPackage", null);

    public ServicePackageSelectorBox(List<ServicePackage> availablePackages) {
        Objects.requireNonNull(availablePackages, "availablePackages");

        packageComboBox.setItems(FXCollections.observableArrayList(availablePackages));
        packageComboBox.setPromptText(lang.get("servicePackage.prompt"));
        packageComboBox.setMaxWidth(Double.MAX_VALUE);
        packageComboBox.setConverter(new StringConverter<ServicePackage>() {
            @Override
            public String toString(ServicePackage pkg) {
                return pkg == null ? "" : pkg.getName();
            }

            @Override
            public ServicePackage fromString(String text) {
                return null;
            }
        });

        packageComboBox.valueProperty().addListener(
                (obs, oldPkg, newPkg) -> selectedPackage.set(newPkg));

        getStyleClass().add("service-package-selector");
        getChildren().add(packageComboBox);
    }

    public ServicePackage getSelectedPackage() {
        return selectedPackage.get();
    }

    public ReadOnlyObjectProperty<ServicePackage> selectedPackageProperty() {
        return selectedPackage.getReadOnlyProperty();
    }
}