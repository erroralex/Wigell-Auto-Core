package com.wac.autocore.view.component;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.LanguageManager;
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
 * Ansvar: Återanvändbar komponent för att välja flera tjänster. Användaren väljer en tjänst
 * i en ComboBox och klickar Lägg till; valda tjänster visas i en {@link MultiSelectListView}
 * med en Ta bort-knapp per rad, och en summering av pris och tid.
 * Komponenten hämtar ingen data själv: den som skapar den skickar in tillgängliga tjänster,
 * och läser resultatet med {@link #getSelectedServiceIds()} som lämnas till servicelagret.
 * Summeringen är bara visning; sluttid och validering räknas i servicelagret.
 * Tjänster jämförs via id, eftersom {@link ServiceItem} saknar equals/hashCode.
 * Samma tjänst kan inte väljas två gånger.
 */
public class ServiceSelectorBox extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private static final String ERROR_STYLE_CLASS = "input-error";

    // Tillgängliga tjänster per id, i den ordning de skickades in
    private final Map<Integer, ServiceItem> servicesById = new LinkedHashMap<>();
    private final FilteredList<ServiceItem> selectableServices;
    private final Set<Integer> selectedIds = new HashSet<>();

    // UI-komponenter
    private final ComboBox<ServiceItem> serviceComboBox = new ComboBox<>();
    private final Button addButton = new Button(lang.get("btn.add"));
    // MultiSelectListView visar redan valda tjänster med Ta bort-knapp per rad
    private final MultiSelectListView<ServiceItem> selectedListView =
            new MultiSelectListView<>(ServiceSelectorBox::describe);
    // Label som visar summering av pris och tid för valda tjänster
    private final Label summaryLabel = new Label();

    // ReadOnlyBooleanWrapper som håller koll på om listan är tom (ingen tjänst vald)
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(this, "empty", true);

    // Tar emot en lista av tillgängliga tjänster. Dubbletter ignoreras.
    public ServiceSelectorBox(List<ServiceItem> availableServices) {
        Objects.requireNonNull(availableServices, "availableServices");
        for (ServiceItem service : availableServices) {
            if (service != null) {
                servicesById.putIfAbsent(service.getId(), service);
            }
        }

        // Skapar en ObservableList av alla tjänster och en FilteredList som används för ComboBox
        ObservableList<ServiceItem> allServices = FXCollections.observableArrayList(servicesById.values());
        selectableServices = new FilteredList<>(allServices);

        // Sätter CSS-klass för styling och anropar setup-metoder för ComboBox och Add-knapp
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

    // Id för valda tjänster i vald ordning. Returnerar en ny kopia vid varje anrop.
    public List<Integer> getSelectedServiceIds() {
        List<Integer> ids = new ArrayList<>();
        for (ServiceItem service : selectedListView.getItems()) {
            ids.add(service.getId());
        }
        return ids;
    }

    // Ersätter nuvarande val, t.ex. för att visa tjänster som redan finns på en bokning.
    // Id som inte finns bland tillgängliga tjänster och dubbletter ignoreras.
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

    // Sant när ingen tjänst är vald. Kan bara läsas, t.ex. för att binda en Spara-knapp.
    public ReadOnlyBooleanProperty emptyProperty() {
        return empty.getReadOnlyProperty();
    }

    public boolean isEmpty() {
        return empty.get();
    }

    // Markerar listan med felram (input-error). Tas bort automatiskt när en tjänst läggs till.
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

    // Konfigurerar ComboBox för att visa tillgängliga tjänster
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

    // Konfigurerar Add-knappen
    private void setupAddButton() {
        addButton.getStyleClass().addAll("btn", "btn-primary");
        addButton.setMinWidth(Region.USE_PREF_SIZE);
        addButton.setTooltip(new Tooltip(lang.get("serviceSelector.addTooltip")));
        addButton.disableProperty().bind(serviceComboBox.valueProperty().isNull());
        addButton.setOnAction(event -> addSelectedService());
    }

    // Lägger till den valda tjänsten i listan om den inte redan finns där
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

    // Håller id-mängd, ComboBox-filter, tom-status, felmarkering och summering synkroniserade med listan
    private void onSelectionChanged() {
        selectedIds.clear();
        int totalMinutes = 0;
        double totalPrice = 0;
        for (ServiceItem service : selectedListView.getItems()) {
            selectedIds.add(service.getId());
            totalMinutes += service.getEstimatedMinutes();
            totalPrice += service.getPrice();
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

    // Returnerar en beskrivning av tjänsten som visas i ComboBox och summering
    private static String describe(ServiceItem service) {
        return service.getName()
                + " – " + lang.get("format.price", service.getPrice())
                + " · " + lang.get("format.minutes", service.getEstimatedMinutes());

    }

}
