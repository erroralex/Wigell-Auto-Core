package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.service.WorkOrderScheduleResult;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

// Dialogen sparar själv via WorkOrderService (samma mönster som feature/movingValidationRulesFromDialogs):
// vid fel, t.ex. en krock på en bekräftad order, visas felet via ErrorFacade och dialogen förblir öppen.
// Resultatet innehåller servicens WorkOrderScheduleResult, så att vyn kan visa en krockvarning för ett utkast.
public class EditWorkOrderDialog extends Dialog<WorkOrderScheduleResult> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final WorkOrderService workOrderService;
    private final WorkOrder workOrder;
    private final ServiceSelectorBox serviceSelectorBox;

    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final DatePicker plannedDatePicker = new DatePicker();
    private final ComboBox<LocalTime> plannedStartTimeComboBox = new ComboBox<>();
    private final TextArea customerInstructionsArea = new TextArea();
    private final TextArea commentsArea = new TextArea();

    private final Map<Integer, CheckBox> chargeBoxes = new LinkedHashMap<>();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private WorkOrderScheduleResult saved;

    public EditWorkOrderDialog(WorkOrder workOrder, List<ServiceItem> serviceItems, List<Mechanic> mechanics,
                               WorkOrderService workOrderService) {
        this(workOrder, serviceItems, mechanics, workOrderService, workOrder.getPlannedStartTime());
    }

    // startTime förifyller fältet, t.ex. med den föreslagna tiden från en krockvarning.
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

        setTitle(lang.get("workOrder.edit.title"));
        setHeaderText(lang.get("workOrder.edit.header"));

        DialogUtil.applyTheme(this);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        setupFields(mechanics, startTime);

        if (workOrder.allowsNonChargeableItems()) {
            for (WorkOrderItem item : workOrder.getItems()) {
                CheckBox cb = new CheckBox(item.getServiceName());
                cb.setSelected(item.isChargeable());
                chargeBoxes.put(item.getServiceItemId(), cb);
            }
        }
        buildContent();

        handleSaveButton();
    }

    private void handleSaveButton() {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(serviceSelectorBox.emptyProperty());

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            Map<Integer, Boolean> flags = new HashMap<>();
            chargeBoxes.forEach((id, checkBox) -> flags.put(id, checkBox.isSelected()));

            try {
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
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
                event.consume();
            }
        });

        setResultConverter(buttonType -> buttonType == saveButtonType ? saved : null);
    }

    private void setupFields(List<Mechanic> mechanics, LocalTime startTime) {
        mechanicComboBox.getItems().setAll(mechanics);
        mechanicComboBox.setMaxWidth(Double.MAX_VALUE);
        mechanicComboBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic mechanic) {
                return mechanic == null ? "" : mechanic.toString();
            }

            @Override
            public Mechanic fromString(String string) {
                return null;
            }
        });

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

    // Hela dygnet i samma steg som WorkOrder föreslår tider i, så att en föreslagen tid alltid går att välja.
    // En befintlig tid utanför stegen (t.ex. från äldre data) läggs till, så att den inte försvinner vid sparning.
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

    private void buildContent() {
        GridPane details = new GridPane();
        details.setHgap(12);
        details.setVgap(8);

        details.add(new Label(lang.get("table.mechanic")), 0, 0);
        details.add(mechanicComboBox, 1, 0);
        details.add(new Label(lang.get("workOrder.edit.plannedDate")), 0, 1);
        details.add(plannedDatePicker, 1, 1);
        details.add(new Label(lang.get("workOrder.edit.plannedStartTime")), 0, 2);
        details.add(plannedStartTimeComboBox, 1, 2);

        VBox content = new VBox(12,
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

        content.setPadding(new Insets(12));
        content.setPrefWidth(560);

        getDialogPane().setContent(content);
    }
}
