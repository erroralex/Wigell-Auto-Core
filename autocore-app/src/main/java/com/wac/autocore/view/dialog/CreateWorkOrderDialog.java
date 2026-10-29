package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderType;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.view.component.CustomerVehiclePicker;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.StringConverterUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
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
import java.util.function.Supplier;

/**
 * <b>CreateWorkOrderDialog</b>
 * <p>Responsibility: Dialog for creating a work order of any of the types in {@link WorkOrderType}.
 * Which fields are shown, what is missing and which result is built are registered per type.
 * For drop-in the user can also register a new customer or a new vehicle via the existing dialogs.
 * The work order itself is created by the view based on {@link Result}.</p>
 * <p>Customer and vehicle are selected via {@link CustomerVehiclePicker}. The rule for the problem description
 * comes from the domain, so the dialog shows the same error that the model throws.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class CreateWorkOrderDialog extends FormDialog<CreateWorkOrderDialog.Result> {

    private final Map<Integer, Vehicle> vehiclesById = new HashMap<>();
    private final Map<Integer, Booking> bookingsById = new HashMap<>();
    private List<Vehicle> vehicles;
    private final StringBinding hint;

    private final ComboBox<WorkOrderType> typeCombo = new ComboBox<>();
    private final ComboBox<Booking> bookingCombo = new ComboBox<>();
    private final Label bookingLabel = new Label(lang.get("workOrder.field.booking"));
    private final Label problemDescriptionLabel = new Label(lang.get("workOrder.problemDescription"));
    private final TextArea problemDescriptionArea = new TextArea();

    private final ComboBox<WorkOrder> originalWorkOrderCombo = new ComboBox<>();
    private final Label originalWorkOrderLabel = new Label(lang.get("workOrder.field.originalWorkOrder"));
    private final Label serviceLabel = new Label(lang.get("workOrder.field.services"));
    private final Label hintLabel = new Label();
    private final ServiceSelectorBox serviceSelector;
    private final CustomerVehiclePicker picker;

    private final Map<WorkOrderType, List<Node>> fieldsByType = new EnumMap<>(WorkOrderType.class);
    private final Map<WorkOrderType, Supplier<String>> hintByType = new EnumMap<>(WorkOrderType.class);
    private final Map<WorkOrderType, Supplier<Result>> resultByType = new EnumMap<>(WorkOrderType.class);

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

        // The same rule controls both the Save button and the explanation of why it is disabled.
        hint = Bindings.createStringBinding(() -> {
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

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "workOrder.new";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("workOrder.create");
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return hint.isNotNull();
    }

    @Override
    protected Node buildContent() {
        VBox content = new VBox();
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
        return content;
    }

    // The domain decides whether the problem description is sufficient. The work order itself
    // is created by the view after the dialog closes.
    @Override
    protected void submit() {
        WorkOrderType type = typeCombo.getValue();
        if (type == null) {
            return;

        }
        WorkOrder.requireProblemDescription(type, problemDescriptionArea.getText());
    }

    @Override
    protected Result buildResult() {
        return resultByType.get(typeCombo.getValue()).get();
    }

    // Everything that differs between the types in the dialog. New type = a new block here.
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

    // Shows or hides nodes and removes them from the layout when they are hidden.
    private static void setShown(boolean shown, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(shown);
            node.setManaged(shown);
        }
    }

    // Returns the translation key for what is missing, or null when the type can be saved.
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

        // Booking date first, otherwise the planned date; if both are missing a translated placeholder is shown.
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

        // Factories instead of a constructor: each type has different required fields.
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
