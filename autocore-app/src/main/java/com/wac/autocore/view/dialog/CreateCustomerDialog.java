package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Customer;
import com.wac.autocore.service.CustomerService;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * <b>CreateCustomerDialog</b>
 * <p>Responsibility: Dialog for creating a new customer.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class CreateCustomerDialog extends FormDialog<Customer> {

    private final CustomerService customerService;
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final CheckBox vipCheckBox = new CheckBox(lang.get("customer.vip"));

    private Customer createdCustomer;

    public CreateCustomerDialog(CustomerService customerService) {
        this.customerService = customerService;
        configureFields();
        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "customer.new";
    }

    @Override
    protected Node buildContent() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");

        grid.add(new Label(lang.get("table.name")), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(lang.get("table.phone")), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(lang.get("table.email")), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(vipCheckBox, 1, 3);

        return grid;
    }

    @Override
    protected void submit() {
        String name = getTrimmedValue(nameField);
        String phone = getTrimmedValue(phoneField);
        String email = getTrimmedValue(emailField);

        createdCustomer = customerService.create(name, phone, email);
        if (vipCheckBox.isSelected()) {
            createdCustomer.setVip(true);
            customerService.update(createdCustomer);
        }
    }

    @Override
    protected Customer buildResult() {
        return createdCustomer;
    }

    private void configureFields() {
        nameField.setPromptText(lang.get("table.name"));
        phoneField.setPromptText(lang.get("table.phone"));
        emailField.setPromptText(lang.get("table.email"));

        nameField.getStyleClass().add("input");
        phoneField.getStyleClass().add("input");
        emailField.getStyleClass().add("input");
    }

    private String getTrimmedValue(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
