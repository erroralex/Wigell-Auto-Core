package com.wac.autocore.view.dialog;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>ServicePackageDialog</b>
 * <p>Ansvar: Samla in och returnera uppgifter för att skapa eller redigera ett servicepaket.
 * Läget väljs med fabriksmetoderna {@link #forCreate} och {@link #forEdit}
 * (<b>Factory Method</b>).</p>
 * <p>Dialogen hanterar endast inmatning och returnerar uppgifterna via {@link Result}.
 * Sparande och felhantering sköts av den anropande vyn.</p>
 * <p>Tjänster väljs med {@link ServiceSelectorBox}, som bygger på MultiSelectListView.</p>
 */
public class ServicePackageDialog extends Dialog<ServicePackageDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();


    private final ServicePackage existingServicePackage;

    private final TextField nameField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ServiceSelectorBox serviceSelector;

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private ServicePackage savedServicePackage;

    // Fabriksmetoder:
    public static ServicePackageDialog forCreate(
            List<ServiceItem> availableServices) {

        return new ServicePackageDialog(availableServices, null);
    }

    public static ServicePackageDialog forEdit(
            List<ServiceItem> availableServices,
            ServicePackage existingServicePackage) {

        return new ServicePackageDialog(
                availableServices,
                Objects.requireNonNull(existingServicePackage, "existingServicePackage")
        );
    }

    private ServicePackageDialog(
            List<ServiceItem> availableServices,
            ServicePackage existingServicePackage) {

        this.existingServicePackage = existingServicePackage;
        this.serviceSelector = new ServiceSelectorBox(availableServices);

        setTitle(lang.get(isEdit() ? "servicePackage.editTitle" : "servicePackage.createTitle"));
        setHeaderText(isEdit() ? existingServicePackage.getName() : null);
        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        buildContent();
        if (isEdit()) {
            prefill();
        }
        setupSaveButton();

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            String description = trim(descriptionArea.getText());
            return new Result(
                    isEdit() ? existingServicePackage.getId() : null,
                    trim(nameField.getText()),
                    description.isEmpty() ? null : description,
                    serviceSelector.getSelectedServiceIds()
            );
        });
    }

    private boolean isEdit() {
        return existingServicePackage != null;
    }

    private void buildContent() {
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        VBox content = new VBox(8,
                new Label(lang.get("table.name")), nameField,
                new Label(lang.get("table.desc")), descriptionArea,
                new Label(lang.get("table.serviceItems")), serviceSelector);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);

        getDialogPane().setContent(content);
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

    private void setupSaveButton() {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);

        BooleanBinding invalid = Bindings.createBooleanBinding(
                () -> trim(nameField.getText()).isEmpty() || serviceSelector.isEmpty(),
                nameField.textProperty(), serviceSelector.emptyProperty());
        saveButton.disableProperty().bind(invalid);
    }

 /*   private ServicePackage save() {
        String name = trim(nameField.getText());
        String description = trim(descriptionArea.getText());
        List<Integer> serviceIds = serviceSelector.getSelectedServiceIds();

        String descriptionOrNull = description.isEmpty() ? null : description;
        if (isEdit()) {
            return servicePackageService.update(existingServicePackage.getId(), name, descriptionOrNull, serviceIds);
        }
        return servicePackageService.create(name, descriptionOrNull, serviceIds);
    }*/

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }

    public static final class Result {
        private final Integer id;
        private final String name;
        private final String description;
        private final List<Integer> serviceItemIds;

        public Result(Integer id, String name, String description, List<Integer> serviceItemIds) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.serviceItemIds = new ArrayList<>(serviceItemIds);
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
        public List<Integer> getServiceItemIds() {
            return new ArrayList<>(serviceItemIds);
        }
    }
}
