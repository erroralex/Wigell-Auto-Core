package com.wac.autocore.view;

import com.wac.autocore.view.i18n.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {

    LanguageManager lang = LanguageManager.getInstance();

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label errorLabel;

    public LoginView() {
        this.usernameField = new TextField();
        this.passwordField = new PasswordField();
        this.loginButton = new Button(lang.get("login.loginButton"));
        this.errorLabel = new Label();

        this.getStyleClass().add("login-view");
        setAlignment(Pos.CENTER);

        usernameField.setPrefWidth(400);
        passwordField.setPrefWidth(400);
        loginButton.setPrefWidth(400);

        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setOpacity(0);
        errorLabel.setMinHeight(20);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setPrefWidth(300);
        errorLabel.setPrefHeight(30);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(Double.MAX_VALUE);


        String cssPath = getClass().getResource("style.css").toExternalForm();
        String loginCssPath = getClass().getResource("login.css").toExternalForm();
        this.getStylesheets().addAll(cssPath, loginCssPath);

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        errorLabel.setOnMouseClicked(e -> hideError());

        VBox panel = new VBox();
        panel.getStyleClass().add("login-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setSpacing(15);
        panel.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);


        panel.getChildren().addAll(usernameField, passwordField, loginButton, errorLabel);

        getChildren().add(panel);
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
        errorLabel.setOpacity(1);
    }

    public void hideError() {
        errorLabel.setOpacity(0);
    }
}