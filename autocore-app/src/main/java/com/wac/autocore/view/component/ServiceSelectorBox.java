package com.wac.autocore.view.component;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.view.i18n.LanguageManager;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.*;

/**
 * ServiceSelectorBox
 * Responsibility: Reusable component for selecting multiple services. The user picks a service
 * in a ComboBox and clicks Add; selected services are shown in a {@link MultiSelectListView}
 * with a Remove button per row, and a summary of price and duration.
 * The component fetches no data itself: whoever creates it passes in the available services,
 * and reads the result with {@link #getSelectedServiceIds()}, which is handed to the service layer.
 * The summary is display only; end time and validation are calculated in the service layer.
 * Services are compared by id, because {@link ServiceItem} lacks equals/hashCode.
 * The same service cannot be selected twice.
 */
public class ServiceSelectorBox extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private static final String ERROR_STYLE_CLASS = "input-error";

    // Available services by id, in the order they were passed in
    private final Map<Integer, ServiceItem> servicesById = new LinkedHashMap<>();
    private final FilteredList<ServiceItem> selectableServices;
    private final Set<Integer> selectedIds = new HashSet<>();

    // UI components
    private final ComboBox<ServiceItem> serviceComboBox = new ComboBox<>();
    private final Button addButton = new Button(lang.get("btn.add"));
    // MultiSelectListView shows the already selected services with a Remove button per row
    private final MultiSelectListView<ServiceItem> selectedListView =
            new MultiSelectListView<>(ServiceSelectorBox::describe);
    // Label that shows a summary of price and duration for the selected services
    private final Label summaryLabel = new Label();

    // ReadOnlyBooleanWrapper that keeps track of whether the list is empty (no service selected)
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(this, "empty", true);

    // Takes a list of available services. Duplicates are ignored.
    public ServiceSelectorBox(List<ServiceItem> availableServices) {
        Objects.requireNonNull(availableServices, "availableServices");
        for (ServiceItem service : availableServices) {
            if (service != null) {
                servicesById.putIfAbsent(service.getId(), service);
            }
        }

        // Creates an ObservableList of all services and a FilteredList used for the ComboBox
        ObservableList<ServiceItem> allServices = FXCollections.observableArrayList(servicesById.values());
        selectableServices = new FilteredList<>(allServices);

        // Sets the CSS class for styling and calls the setup methods for the ComboBox and Add button
        getStyleClass().add("service-selector");
        setupComboBox();
        setupAddButton();
        selectedListView.setPlaceholderText(lang.get("serviceSelector.empty"));
        summaryLabel.getStyleClass().addAll("text-secondary", "service-selector-summary");

        HBox picker = new HBox(serviceComboBox, addButton);
        picker.getStyleClass().add("service-selector-picker");
        HBox.setHgrow(serviceComboBox, Priority.ALWAYS);

        getChildren().addAll(picker, selectedListView, summaryLabel);
        VBox.setVgrow(selectedListView, Priority.ALWAYS);

        selectedListView.getItems().addListener(
                (ListChangeListener<ServiceItem>) change -> onSelectionChanged());
        onSelectionChanged();
    }

    // Ids of the selected services in the selected order. Returns a new copy on each call.
    public List<Integer> getSelectedServiceIds() {
        List<Integer> ids = new ArrayList<>();
        for (ServiceItem service : selectedListView.getItems()) {
            ids.add(service.getId());
        }
        return ids;
    }

    // Replaces the current selection, e.g. to show services that already exist on a booking.
    // Ids that are not among the available services, and duplicates, are ignored.
    public void selectServiceIds(Collection<Integer> ids) {
        List<ServiceItem> services = new ArrayList<>();
        Set<Integer> added = new HashSet<>();
        if (ids != null) {
            for (Integer id : ids) {
                ServiceItem service = id == null ? null : servicesById.get(id);
                if (service != null && added.add(id)) {
                    services.add(service);
                }
            }
        }
        selectedListView.getItems().setAll(services);
    }

    // True when no service is selected. Read-only, e.g. for binding a Save button.
    public ReadOnlyBooleanProperty emptyProperty() {
        return empty.getReadOnlyProperty();
    }

    public boolean isEmpty() {
        return empty.get();
    }

    // Marks the list with an error border (input-error). Removed automatically when a service is added.
    public void setError(boolean error) {
        List<String> styleClasses = selectedListView.getStyleClass();
        if (error) {
            if (!styleClasses.contains(ERROR_STYLE_CLASS)) {
                styleClasses.add(ERROR_STYLE_CLASS);
            }
        } else {
            styleClasses.removeAll(Collections.singleton(ERROR_STYLE_CLASS));
        }
    }

    // Configures the ComboBox to show the available services
    private void setupComboBox() {
        serviceComboBox.setItems(selectableServices);
        serviceComboBox.setPromptText(lang.get("serviceSelector.prompt"));
        serviceComboBox.setMaxWidth(Double.MAX_VALUE);
        serviceComboBox.disableProperty().bind(Bindings.isEmpty(selectableServices));
        serviceComboBox.setConverter(new StringConverter<ServiceItem>() {
            @Override
            public String toString(ServiceItem service) {
                return service == null ? "" : describe(service);
            }

            @Override
            public ServiceItem fromString(String text) {
                return null;
            }
        });
    }

    // Configures the Add button
    private void setupAddButton() {
        addButton.getStyleClass().addAll("btn", "btn-primary");
        addButton.setMinWidth(Region.USE_PREF_SIZE);
        addButton.setTooltip(new Tooltip(lang.get("serviceSelector.addTooltip")));
        addButton.disableProperty().bind(serviceComboBox.valueProperty().isNull());
        addButton.setOnAction(event -> addSelectedService());
    }

    // Adds the selected service to the list if it is not already there
    private void addSelectedService() {
        ServiceItem service = serviceComboBox.getValue();
        if (service == null) {
            return;
        }
        if (!selectedIds.contains(service.getId())) {
            selectedListView.getItems().add(service);
        }
        serviceComboBox.setValue(null);
    }

    // Keeps the id set, ComboBox filter, empty status, error marking and summary in sync with the list
    private void onSelectionChanged() {
        selectedIds.clear();
        int totalMinutes = 0;
        Money totalPrice = Money.ZERO;
        for (ServiceItem service : selectedListView.getItems()) {
            selectedIds.add(service.getId());
            totalMinutes += service.getEstimatedMinutes();
            totalPrice = totalPrice.add(service.getPrice());
        }

        Set<Integer> excludedIds = new HashSet<>(selectedIds);
        selectableServices.setPredicate(service -> !excludedIds.contains(service.getId()));

        ServiceItem pending = serviceComboBox.getValue();
        if (pending != null && excludedIds.contains(pending.getId())) {
            serviceComboBox.setValue(null);
        }

        empty.set(selectedIds.isEmpty());
        if (!selectedIds.isEmpty()) {
            setError(false);
        }

        summaryLabel.setText(lang.get("serviceSelector.summary",
                lang.get("format.price", totalPrice),
                lang.get("format.minutes", totalMinutes)));
    }

    // Returns a description of the service shown in the ComboBox and the summary
    private static String describe(ServiceItem service) {
        return service.getName()
                + " – " + lang.get("format.price", service.getPrice())
                + " · " + lang.get("format.minutes", service.getEstimatedMinutes());

    }

}
