package com.wac.autocore.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label errorLabel;

    public LoginView() {
        this.usernameField = new TextField();
        this.passwordField = new PasswordField();
        this.loginButton = new Button("Log in");
        this.errorLabel = new Label();

        setAlignment(Pos.CENTER);
        setSpacing(15);

        usernameField.setMaxWidth(300);
        passwordField.setMaxWidth(300);
        loginButton.setMaxWidth(300);

        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        // Hide error when editing the fields
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());

        errorLabel.setOnMouseClicked(e -> {hideError();});
        
        getChildren().addAll(usernameField, passwordField, loginButton, errorLabel);
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public void showError(String messageKey) {
        errorLabel.setText(messageKey);
        errorLabel.setVisible(true);
    }

    public void hideError() {
        errorLabel.setVisible(false);
    }
}