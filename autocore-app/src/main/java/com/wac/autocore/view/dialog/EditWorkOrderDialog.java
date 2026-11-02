package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.service.WorkOrderScheduleResult;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.StringConverterUtil;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <b>EditWorkOrderDialog</b>
 * <p>Responsibility: Dialog for editing a work order's schedule, services and notes. The dialog saves
 * by itself and returns the service's {@link WorkOrderScheduleResult}, so the view can show a clash
 * warning for a draft.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class EditWorkOrderDialog extends FormDialog<WorkOrderScheduleResult> {

    private final WorkOrderService workOrderService;
    private final WorkOrder workOrder;
    private final ServiceSelectorBox serviceSelectorBox;

    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final DatePicker plannedDatePicker = new DatePicker();
    private final ComboBox<LocalTime> plannedStartTimeComboBox = new ComboBox<>();
    private final TextArea customerInstructionsArea = new TextArea();
    private final TextArea commentsArea = new TextArea();

    private final Map<Integer, CheckBox> chargeBoxes = new LinkedHashMap<>();

    private WorkOrderScheduleResult saved;

    public EditWorkOrderDialog(WorkOrder workOrder, List<ServiceItem> serviceItems, List<Mechanic> mechanics,
                               WorkOrderService workOrderService) {
        this(workOrder, serviceItems, mechanics, workOrderService, workOrder.getPlannedStartTime());
    }

    // startTime prefills the field, e.g. with the suggested time from a clash warning.
    public EditWorkOrderDialog(WorkOrder workOrder, List<ServiceItem> serviceItems, List<Mechanic> mechanics,
                               WorkOrderService workOrderService, LocalTime startTime) {
        this.workOrder = workOrder;
        this.workOrderService = workOrderService;

        serviceSelectorBox = new ServiceSelectorBox(serviceItems);

        serviceSelectorBox.selectServiceIds(
                workOrder.getItems().stream()
                        .map(WorkOrderItem::getServiceItemId)
                        .collect(Collectors.toList())
        );

        setupFields(mechanics, startTime);

        createChargeBoxes();

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "workOrder.edit.title";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("workOrder.edit.header");
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return serviceSelectorBox.emptyProperty();
    }

    @Override
    protected Node buildContent() {
        GridPane details = new GridPane();
        details.getStyleClass().add("form-grid");

        details.add(new Label(lang.get("table.mechanic")), 0, 0);
        details.add(mechanicComboBox, 1, 0);
        details.add(new Label(lang.get("workOrder.edit.plannedDate")), 0, 1);
        details.add(plannedDatePicker, 1, 1);
        details.add(new Label(lang.get("workOrder.edit.plannedStartTime")), 0, 2);
        details.add(plannedStartTimeComboBox, 1, 2);

        VBox content = new VBox(
                details,
                new Label(lang.get("workOrder.edit.customerInstructions")),
                customerInstructionsArea,
                new Label(lang.get("workOrder.edit.comments")),
                commentsArea,
                new Separator(),
                new Label(lang.get("table.serviceItems")),
                serviceSelectorBox
        );

        if (!chargeBoxes.isEmpty()) {
            content.getChildren().add(new Label(lang.get("workOrder.edit.chargeCustomer")));
            content.getChildren().addAll(chargeBoxes.values());
        }

        content.setPrefWidth(560);

        return content;
    }

    @Override
    protected void submit() {
        Map<Integer, Boolean> flags = new HashMap<>();
        chargeBoxes.forEach((id, checkBox) -> flags.put(id, checkBox.isSelected()));

        Mechanic mechanic = mechanicComboBox.getValue();

        saved = workOrderService.updateWorkOrder(
                workOrder.getId(),
                serviceSelectorBox.getSelectedServiceIds(),
                mechanic == null ? null : mechanic.getId(),
                plannedDatePicker.getValue(),
                plannedStartTimeComboBox.getValue(),
                customerInstructionsArea.getText(),
                commentsArea.getText(),
                flags
        );
    }

    @Override
    protected WorkOrderScheduleResult buildResult() {
        return saved;
    }

    private void createChargeBoxes() {
        if (workOrder.allowsNonChargeableItems()) {
            for (WorkOrderItem item : workOrder.getItems()) {
                CheckBox checkBox = new CheckBox(item.getServiceName());
                checkBox.setSelected(item.isChargeable());
                chargeBoxes.put(item.getServiceItemId(), checkBox);
            }
        }
    }

    private void setupFields(List<Mechanic> mechanics, LocalTime startTime) {
        mechanicComboBox.getItems().setAll(mechanics);
        mechanicComboBox.setMaxWidth(Double.MAX_VALUE);
        mechanicComboBox.setConverter(StringConverterUtil.display(Mechanic::getName));

        if (workOrder.getMechanicId() != null) {
            mechanicComboBox.getItems().stream()
                    .filter(m -> Objects.equals(m.getId(), workOrder.getMechanicId()))
                    .findFirst()
                    .ifPresent(mechanicComboBox::setValue);
        }

        plannedDatePicker.setValue(workOrder.getPlannedDate());
        plannedDatePicker.setMaxWidth(Double.MAX_VALUE);

        plannedStartTimeComboBox.getItems().setAll(startTimes(startTime));
        plannedStartTimeComboBox.setValue(startTime);
        plannedStartTimeComboBox.setMaxWidth(Double.MAX_VALUE);

        customerInstructionsArea.setText(workOrder.getCustomerInstructions());
        customerInstructionsArea.setPromptText(lang.get("workOrder.edit.customerInstructionsPrompt"));
        customerInstructionsArea.setWrapText(true);
        customerInstructionsArea.setPrefRowCount(3);

        commentsArea.setText(workOrder.getComments());
        commentsArea.setPromptText(lang.get("workOrder.edit.commentsPrompt"));
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(3);
    }

    // The whole day in the same step that WorkOrder suggests times in, so a suggested time can always be selected.
    // An existing time outside the steps (e.g. from older data) is added, so it does not disappear on save.
    private static List<LocalTime> startTimes(LocalTime current) {
        TreeSet<LocalTime> times = new TreeSet<>();
        LocalTime time = LocalTime.MIDNIGHT;
        do {
            times.add(time);
            time = time.plusMinutes(WorkOrder.SUGGESTION_STEP_MINUTES);
        } while (!time.equals(LocalTime.MIDNIGHT));
        if (current != null) {
            times.add(current);
        }
        return new ArrayList<>(times);
    }
}
