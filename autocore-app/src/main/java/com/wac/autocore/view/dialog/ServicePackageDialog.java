package com.wac.autocore.view.dialog;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.component.ServiceSelectorBox;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>ServicePackageDialog</b>
 * <p>Responsibility: Create or edit a service package. The same dialog for both modes; the mode is chosen with
 * the factory methods {@link #forCreate} and {@link #forEdit} (<b>Factory Method</b>).</p>
 * <p>Services are selected with {@link ServiceSelectorBox}, which is built on MultiSelectListView.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class ServicePackageDialog extends FormDialog<ServicePackage> {

    private final ServicePackageService servicePackageService;
    private final ServicePackage existingServicePackage;

    private final TextField nameField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ServiceSelectorBox serviceSelector;

    private ServicePackage savedServicePackage;

    public static ServicePackageDialog forCreate(
            ServicePackageService servicePackageService,
            List<ServiceItem> availableServices) {

        return new ServicePackageDialog(servicePackageService, availableServices, null);
    }

    public static ServicePackageDialog forEdit(
            ServicePackageService servicePackageService,
            List<ServiceItem> availableServices,
            ServicePackage existingServicePackage) {

        return new ServicePackageDialog(
                servicePackageService,
                availableServices,
                Objects.requireNonNull(existingServicePackage, "existingServicePackage")
        );
    }

    private ServicePackageDialog(
            ServicePackageService servicePackageService,
            List<ServiceItem> availableServices,
            ServicePackage existingServicePackage) {

        this.servicePackageService = servicePackageService;
        this.existingServicePackage = existingServicePackage;
        this.serviceSelector = new ServiceSelectorBox(availableServices);

        if (isEdit()) {
            prefill();
        }

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return isEdit() ? "servicePackage.editTitle" : "servicePackage.createTitle";
    }

    @Override
    protected String buildHeaderText() {
        return isEdit() ? existingServicePackage.getName() : null;
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return Bindings.createBooleanBinding(
                () -> trim(nameField.getText()).isEmpty() || serviceSelector.isEmpty(),
                nameField.textProperty(), serviceSelector.emptyProperty());
    }

    @Override
    protected Node buildContent() {
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        VBox content = new VBox(8,
                new Label(lang.get("table.name")), nameField,
                new Label(lang.get("table.desc")), descriptionArea,
                new Label(lang.get("table.serviceItems")), serviceSelector);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);

        return content;
    }

    @Override
    protected void submit() {
        savedServicePackage = save();
    }

    @Override
    protected ServicePackage buildResult() {
        return savedServicePackage;
    }

    private boolean isEdit() {
        return existingServicePackage != null;
    }

    private void prefill() {
        nameField.setText(existingServicePackage.getName());
        descriptionArea.setText(existingServicePackage.getDescription());

        List<Integer> serviceIds = new ArrayList<>();
        for (ServiceItem service : existingServicePackage.getServices()) {
            serviceIds.add(service.getId());
        }
        serviceSelector.selectServiceIds(serviceIds);
    }

    private ServicePackage save() {
        String name = trim(nameField.getText());
        String description = trim(descriptionArea.getText());
        List<Integer> serviceIds = serviceSelector.getSelectedServiceIds();

        String descriptionOrNull = description.isEmpty() ? null : description;
        if (isEdit()) {
            return servicePackageService.update(existingServicePackage.getId(), name, descriptionOrNull, serviceIds);
        }
        return servicePackageService.create(name, descriptionOrNull, serviceIds);
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
