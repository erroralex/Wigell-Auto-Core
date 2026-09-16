package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>CreateInvoiceDialog</b>
 * <p>Ansvar: Dialog för att skapa en faktura.</p>
 */
public class CreateInvoiceDialog extends Dialog<CreateInvoiceDialog.Result> {

    private final ComboBox<WorkOrder> workOrderCombo = new ComboBox<>();
    private final TextField discountField = new TextField();

    private final ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

    public CreateInvoiceDialog() {
        setTitle("New Invoice");
        setHeaderText("Create Invoice");

        getDialogPane().getStylesheets().add(
                getClass().getResource("/com/wac/autocore/view/style.css").toExternalForm()
        );

        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // -------------------------------------------------------------------------------------------------------------

        List<WorkOrder> completed = Database.getWorkOrders().stream()
                .filter(wo -> "COMPLETED".equals(wo.getStatus()))
                .collect(Collectors.toList());
        workOrderCombo.getItems().addAll(completed);

        workOrderCombo.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder wo) {
                return wo == null ? "" : "#" + wo.getId() + " (" + wo.getStatus() + ")";
            }

            @Override
            public WorkOrder fromString(String string) {
                return null;
            }
        });

        // -------------------------------------------------------------------------------------------------------------

        discountField.setPromptText("Discount code (optional)");

        // -------------------------------------------------------------------------------------------------------------

        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        workOrderCombo.valueProperty().addListener((obs, oldV, newV) -> validate(saveButton));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("Work order"), workOrderCombo,
                new Label("Discount code"), discountField
        );
        getDialogPane().setContent(content);

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                int workOrderId = workOrderCombo.getValue().getId();
                String code = discountField.getText();
                return new Result(workOrderId, code);
            }
            return null;
        });
    }

    private void validate(Node saveButton) {
        boolean valid = workOrderCombo.getValue() != null;
        saveButton.setDisable(!valid);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static class Result {
        private final int workOrderId;
        private final String discountCode;

        public Result(int workOrderId, String discountCode) {
            this.workOrderId = workOrderId;
            this.discountCode = discountCode;
        }

        public int getWorkOrderId() {
            return workOrderId;
        }

        public String getDiscountCode() {
            return discountCode;
        }
    }
}