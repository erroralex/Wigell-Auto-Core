package com.wac.autocore.view.dialog;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.model.*;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.WorkOrderView;
import com.wac.autocore.view.i18n.LanguageManager;
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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

/**
 * <b>CreateWorkOrderDialog</b>
 * <p>Ansvar: Dialog för att skapa en arbetsorder av någon av typerna i {@link WorkOrderType}.
 * Vilka fält som visas, vad som saknas och vilket resultat som byggs registreras per typ.
 * För drop-in kan användaren även registrera en ny kund eller ett nytt fordon via de befintliga dialogerna.
 * Själva arbetsordern skapas av vyn utifrån {@link Result}.</p>
 * <p>Kund och fordon väljs via {@link CustomerVehiclePicker}. Regeln för problembeskrivning
 * kommer från domänen, så dialogen visar samma fel som modellen kastar.</p>
 */
public class CreateWorkOrderDialog extends Dialog<CreateWorkOrderDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final Map<Integer, Vehicle> vehiclesById = new HashMap<>();
    private final Map<Integer, Booking> bookingsById = new HashMap<>();
    private List<Vehicle> vehicles;

    private final WorkOrderService workOrderService;
    private final ComboBox<WorkOrderType> typeCombo = new ComboBox<>();
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

    private final Map<WorkOrderType, List<Node>> fieldsByType = new EnumMap<>(WorkOrderType.class);
    private final Map<WorkOrderType, Supplier<String>> hintByType = new EnumMap<>(WorkOrderType.class);
    private final Map<WorkOrderType, Supplier<Result>> resultByType = new EnumMap<>(WorkOrderType.class);

    public CreateWorkOrderDialog(List<Booking> bookings,
                                 List<Booking> allBookings,
                                 List<WorkOrder> completedWorkOrders,
                                 List<ServiceItem> serviceItems,
                                 WorkOrderService workOrderService,
                                 CustomerService customerService,
                                 VehicleService vehicleService) {

        this.serviceSelector = new ServiceSelectorBox(serviceItems);
        this.vehicles = vehicleService.findAll();
        indexVehicles();
        this.picker = new CustomerVehiclePicker(customerService, vehicleService);
        for (Booking booking : allBookings) {
            bookingsById.put(booking.getId(), booking);
        }

        this.workOrderService = workOrderService;

        setTitle(lang.get("workOrder.new"));
        setHeaderText(lang.get("workOrder.create"));

        // Hämta css-styling och applicera på nya dialog:
        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        problemDescriptionArea.setPromptText(lang.get("workOrder.problemDescription.prompt"));
        problemDescriptionArea.setWrapText(true);

        hintLabel.getStyleClass().add("text-secondary");
        hintLabel.setWrapText(true);

        registerTypes();

        typeCombo.getItems().addAll(WorkOrderType.values());

        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setConverter(StringConverterUtil.display(type -> lang.get(type.getMessageKey())));

        typeCombo.valueProperty().addListener((observable, oldType, newType) -> {
            for (List<Node> nodes : fieldsByType.values()) {
                setShown(false, nodes.toArray(new Node[0]));
            }
            if (newType != null) {
                setShown(true, fieldsByType.get(newType).toArray(new Node[0]));
            }
        });

        typeCombo.setValue(WorkOrderType.PLANNED);

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
        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        saveButton.disableProperty().bind(hint.isNotNull());
        saveButton.getStyleClass().addAll("btn", "btn-primary");
        cancelButton.getStyleClass().addAll("btn", "btn-secondary");

        // Domänen avgör om problembeskrivningen räcker. Vid fel visas domänens meddelande
        // och dialogen hålls öppen så att användaren inte förlorar det som fyllts i.
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            WorkOrderType type = typeCombo.getValue();
            if (type == null) {
                event.consume();
                return;
            }


            try {
                //WorkOrder.requireProblemDescription(type, problemDescriptionArea.getText());
                switch (type) {
                    case PLANNED:
                        workOrderService.createWorkOrder(
                                bookingCombo.getValue().getId());
                                break;
                    case DROP_IN:
                        workOrderService.createDropIn(
                                picker.getSelectedVehicle().getId(),
                                problemDescriptionArea.getText(),
                                serviceSelector.getSelectedServiceIds()
                        );
                        break;
                    case WARRANTY:
                        workOrderService.createWarranty(
                                originalWorkOrderCombo.getValue().getId(),
                                problemDescriptionArea.getText()
                        );
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unknown work order type: " + type
                        );
                }
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
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
            return resultByType.get(typeCombo.getValue()).get();
        });
    }

    // Allt som skiljer typerna åt i dialogen. Ny typ = ett nytt block här.
    private void registerTypes() {
        fieldsByType.put(WorkOrderType.PLANNED, Arrays.asList(bookingLabel, bookingCombo));
        hintByType.put(WorkOrderType.PLANNED,
                () -> bookingCombo.getValue() == null ? "workOrder.hint.selectBooking" : null);
        resultByType.put(WorkOrderType.PLANNED,
                () -> Result.planned(bookingCombo.getValue()));

        fieldsByType.put(WorkOrderType.DROP_IN, Arrays.asList(picker, problemDescriptionLabel, problemDescriptionArea, serviceLabel, serviceSelector));
        hintByType.put(WorkOrderType.DROP_IN,
                () -> picker.missingSelectionKeyBinding().get());
        resultByType.put(WorkOrderType.DROP_IN,
                () -> Result.dropIn(picker.getSelectedVehicle(),
                        problemDescriptionArea.getText(),
                        serviceSelector.getSelectedServiceIds()));

        fieldsByType.put(WorkOrderType.WARRANTY, Arrays.asList(originalWorkOrderLabel, originalWorkOrderCombo, problemDescriptionLabel, problemDescriptionArea));
        hintByType.put(WorkOrderType.WARRANTY, () -> {
            if (originalWorkOrderCombo.getItems().isEmpty()) {
                return "workOrder.hint.noCompleted";
            }
            return originalWorkOrderCombo.getValue() == null ? "workOrder.hint.selectOriginal" : null;
        });
        resultByType.put(WorkOrderType.WARRANTY,
                () -> Result.warranty(originalWorkOrderCombo.getValue(),
                        problemDescriptionArea.getText()));

        requireAllTypesRegistered();
    }

    private void requireAllTypesRegistered() {
        for (WorkOrderType type : WorkOrderType.values()) {
            if (!fieldsByType.containsKey(type)
                    || !hintByType.containsKey(type)
                    || !resultByType.containsKey(type)) {
                throw new IllegalStateException("Work order type not registered in dialog: " + type);
            }
        }
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
        WorkOrderType type = typeCombo.getValue();
        if (type == null) {
            return "workOrder.hint.selectType";
        }
        return hintByType.get(type).get();
    }

    private void indexVehicles() {
        vehiclesById.clear();
        for (Vehicle vehicle : vehicles) {
            vehiclesById.put(vehicle.getId(), vehicle);
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
        private final WorkOrderType type;
        private final Booking booking;
        private final Vehicle vehicle;
        private final WorkOrder originalWorkOrder;
        private final String problemDescription;
        private final List<Integer> serviceItemIds;

        // Fabriker i stället för konstruktor: varje typ har olika obligatoriska fält.
        private Result(WorkOrderType type,
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
            return new Result(WorkOrderType.PLANNED, booking, null, null, null, Collections.emptyList());
        }

        public static Result dropIn(Vehicle vehicle, String problemDescription, List<Integer> serviceItemIds) {
            return new Result(WorkOrderType.DROP_IN, null, vehicle, null, problemDescription, serviceItemIds);
        }

        public static Result warranty(WorkOrder original, String problemDescription) {
            return new Result(WorkOrderType.WARRANTY, null, null, original, problemDescription, Collections.emptyList());
        }

        public WorkOrderType getType() {
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
