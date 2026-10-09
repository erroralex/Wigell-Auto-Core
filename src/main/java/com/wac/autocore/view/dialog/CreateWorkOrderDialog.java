package com.wac.autocore.view.dialog;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.view.component.CustomerVehiclePicker;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import com.wac.autocore.view.util.StringConverterUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>CreateWorkOrderDialog</b>
 * <p>Ansvar: Dialog för att skapa en arbetsorder av någon av typerna planerad, drop-in
 * eller reklamation. Vilka fält som visas styrs av vald typ. För drop-in kan användaren
 * även registrera en ny kund eller ett nytt fordon via de befintliga dialogerna.
 * Själva arbetsordern skapas av vyn utifrån {@link Result}.</p>
 * <p>Kund och fordon väljs via {@link CustomerVehiclePicker}</p>
 */
public class CreateWorkOrderDialog extends Dialog<CreateWorkOrderDialog.Result> {

    public enum Type {
        PLANNED(false),
        DROP_IN(true),
        WARRANTY(true);

        private final boolean needsProblemDescription;

        Type(boolean needsProblemDescription) {
            this.needsProblemDescription = needsProblemDescription;
        }

        public boolean needsProblemDescription() {
            return needsProblemDescription;
        }
    }

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final Map<Integer, Vehicle> vehiclesById = new HashMap<>();
    private final Map<Integer, Booking> bookingsById = new HashMap<>();
    private List<Vehicle> vehicles;

    private final ComboBox<Type> typeCombo = new ComboBox<>();
    private final ComboBox<Booking> bookingCombo = new ComboBox<>();
    private final Label bookingLabel = new Label(lang.get("workOrder.field.booking"));
    private final Label problemDescriptionLabel = new Label(lang.get("workOrder.problemDescription"));
    private final TextArea problemDescriptionArea = new TextArea();

    private final ComboBox<WorkOrder> originalWorkOrderCombo = new ComboBox<>();
    private final Label originalWorkOrderLabel = new Label(lang.get("workOrder.field.originalWorkOrder"));
    private final Label serviceLabel = new Label(lang.get("workOrder.field.services"));
    private final Label hintLabel = new Label();
    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
    private final ServiceSelectorBox serviceSelector;
    private final CustomerVehiclePicker picker;

    public CreateWorkOrderDialog(List<Booking> bookings,
                                 List<Booking> allBookings,
                                 List<WorkOrder> completedWorkOrders,
                                 List<ServiceItem> serviceItems,
                                 CustomerService customerService,
                                 VehicleService vehicleService) {

        this.serviceSelector = new ServiceSelectorBox(serviceItems);
        this.vehicles = vehicleService.findAll();
        indexVehicles();
        this.picker = new CustomerVehiclePicker(customerService, vehicleService);
        for (Booking booking : allBookings) {
            bookingsById.put(booking.getId(), booking);
        }

        setTitle(lang.get("workOrder.new"));
        setHeaderText(lang.get("workOrder.create"));

        // Hämta css-styling och applicera på nya dialog:
        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        problemDescriptionArea.setPromptText(lang.get("workOrder.problemDescription.prompt"));
        problemDescriptionArea.setWrapText(true);

        hintLabel.getStyleClass().add("text-secondary");
        hintLabel.setWrapText(true);

        // Fält som hör till respektive typ. Ny typ = ny rad här.
        Map<Type, List<Node>> fieldsByType = new EnumMap<>(Type.class);
        fieldsByType.put(Type.PLANNED, Arrays.asList(bookingLabel, bookingCombo));
        fieldsByType.put(Type.DROP_IN, Arrays.asList(picker, problemDescriptionLabel, problemDescriptionArea, serviceLabel, serviceSelector));
        fieldsByType.put(Type.WARRANTY, Arrays.asList(originalWorkOrderLabel, originalWorkOrderCombo, problemDescriptionLabel, problemDescriptionArea));

        typeCombo.getItems().addAll(Type.values());

        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setConverter(StringConverterUtil.display(CreateWorkOrderDialog::typeLabel));

        typeCombo.valueProperty().addListener((observable, oldType, newType) -> {
            for (List<Node> nodes : fieldsByType.values()) {
                setShown(false, nodes.toArray(new Node[0]));
            }
            if (newType != null) {
                setShown(true, fieldsByType.get(newType).toArray(new Node[0]));
            }
        });

        typeCombo.setValue(Type.PLANNED);

        bookingCombo.getItems().addAll(bookings);
        bookingCombo.setPromptText(lang.get("workOrder.field.booking"));
        bookingCombo.setMaxWidth(Double.MAX_VALUE);
        bookingCombo.setConverter(StringConverterUtil.display(
                booking -> "#" + booking.getId() + " - " + booking.getDescription()));

        originalWorkOrderCombo.getItems().addAll(completedWorkOrders);
        originalWorkOrderCombo.setPromptText(lang.get(completedWorkOrders.isEmpty()
                ? "workOrder.hint.noCompleted"
                : "workOrder.field.originalWorkOrder"));
        originalWorkOrderCombo.setMaxWidth(Double.MAX_VALUE);
        originalWorkOrderCombo.setConverter(StringConverterUtil.display(this::describeOriginalWorkOrder));

        // Samma regel styr både Spara-knappen och förklaringen till varför den är avstängd.
        StringBinding hint = Bindings.createStringBinding(() -> {
                    String key = missingSelectionKey();
                    return key == null ? null : lang.get(key);
                },
                typeCombo.valueProperty(),
                bookingCombo.valueProperty(),
                picker.missingSelectionKeyBinding(),
                originalWorkOrderCombo.valueProperty()
        );
        hintLabel.textProperty().bind(hint);
        hintLabel.visibleProperty().bind(hint.isNotNull());
        hintLabel.managedProperty().bind(hintLabel.visibleProperty());

        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(hint.isNotNull());

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            Type type = typeCombo.getValue();
            String description = problemDescriptionArea.getText();

            if (type != null && type.needsProblemDescription()
                    && (description == null || description.trim().isEmpty())) {
                ErrorFacade.handle(
                        new ValidationException("error.workOrder.missingProblemDescription")
                );
                event.consume();
            }
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label(lang.get("workOrder.type")),
                typeCombo,
                bookingLabel,
                bookingCombo,
                picker,
                originalWorkOrderLabel,
                originalWorkOrderCombo,
                problemDescriptionLabel,
                problemDescriptionArea,
                serviceLabel,
                serviceSelector,
                hintLabel
        );

        getDialogPane().setContent(content);

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            switch (typeCombo.getValue()) {
                case PLANNED:
                    return Result.planned(bookingCombo.getValue());
                case DROP_IN:
                    return Result.dropIn(picker.getSelectedVehicle(),
                            problemDescriptionArea.getText(),
                            serviceSelector.getSelectedServiceIds());
                case WARRANTY:
                    return Result.warranty(originalWorkOrderCombo.getValue(),
                            problemDescriptionArea.getText());
                default:
                    throw new IllegalStateException("Unknown work order type: " + typeCombo.getValue());
            }
        });
    }

    // Visar eller döljer noder och tar bort dem ur layouten när de är dolda.
    private static void setShown(boolean shown, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(shown);
            node.setManaged(shown);
        }
    }

    // Returnerar översättningsnyckel för det som saknas, eller null när typen går att spara.
    private String missingSelectionKey() {
        Type type = typeCombo.getValue();
        if (type == null) {
            return "workOrder.hint.selectType";
        }

        switch (type) {
            case PLANNED:
                return bookingCombo.getValue() == null ? "workOrder.hint.selectBooking" : null;
            case DROP_IN:
                return picker.missingSelectionKeyBinding().get();
            case WARRANTY:
                if (originalWorkOrderCombo.getItems().isEmpty()) {
                    return "workOrder.hint.noCompleted";
                }
                return originalWorkOrderCombo.getValue() == null ? "workOrder.hint.selectOriginal" : null;
            default:
                return "workOrder.hint.selectType";
        }
    }

    private void indexVehicles() {
        vehiclesById.clear();
        for (Vehicle vehicle : vehicles) {
            vehiclesById.put(vehicle.getId(), vehicle);
        }
    }

    private static String typeLabel(Type type) {
        switch (type) {
            case PLANNED:
                return lang.get("workOrder.type.planned");
            case DROP_IN:
                return lang.get("workOrder.type.dropIn");
            case WARRANTY:
                return lang.get("workOrder.type.warranty");
            default:
                throw new IllegalArgumentException("Unknown work order type: " + type);
        }
    }

    private String describeOriginalWorkOrder(WorkOrder workOrder) {
        if (workOrder == null) {
            return "";
        }

        Vehicle vehicle = workOrder.getVehicleId() == null ? null : vehiclesById.get(workOrder.getVehicleId());
        String vehicleText = vehicle == null ? lang.get("workOrder.unknownVehicle") : CustomerVehiclePicker.describeVehicle(vehicle);

        Booking booking = workOrder.getBookingId() == null ? null : bookingsById.get(workOrder.getBookingId());

        // Bokningsdatum först, annars planerat datum; saknas båda visas en översatt platshållare.
        LocalDate date = booking != null ? booking.getDate() : null;
        if (date == null) {
            date = workOrder.getPlannedDate();
        }

        String dateText = date == null ? lang.get("workOrder.dateUnavailable") : date.toString();
        return "#" + workOrder.getId() + " - " + vehicleText + " - " + dateText;
    }

    public static class Result {
        private final Type type;
        private final Booking booking;
        private final Vehicle vehicle;
        private final WorkOrder originalWorkOrder;
        private final String problemDescription;
        private final List<Integer> serviceItemIds;

        // Fabriker i stället för konstruktor: varje typ har olika obligatoriska fält.
        private Result(Type type,
                       Booking booking,
                       Vehicle vehicle,
                       WorkOrder originalWorkOrder,
                       String problemDescription,
                       List<Integer> serviceItemIds) {
            this.type = type;
            this.booking = booking;
            this.vehicle = vehicle;
            this.originalWorkOrder = originalWorkOrder;
            this.problemDescription = problemDescription == null ? null : problemDescription.trim();
            this.serviceItemIds = serviceItemIds;
        }

        public static Result planned(Booking booking) {
            return new Result(Type.PLANNED, booking, null, null, null, Collections.emptyList());
        }

        public static Result dropIn(Vehicle vehicle, String problemDescription, List<Integer> serviceItemIds) {
            return new Result(Type.DROP_IN, null, vehicle, null, problemDescription, serviceItemIds);
        }

        public static Result warranty(WorkOrder original, String problemDescription) {
            return new Result(Type.WARRANTY, null, null, original, problemDescription, Collections.emptyList());
        }

        public Type getType() {
            return type;
        }

        public Booking getBooking() {
            return booking;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public WorkOrder getOriginalWorkOrder() {
            return originalWorkOrder;
        }

        public String getProblemDescription() {
            return problemDescription;
        }

        public List<Integer> getServiceItemIds() {
            return serviceItemIds;
        }
    }
}
