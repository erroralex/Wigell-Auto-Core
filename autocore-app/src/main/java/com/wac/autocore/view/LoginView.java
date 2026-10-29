package com.wac.autocore.view;

import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.Stylesheets;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LoginView extends VBox {

    private final LanguageManager lang = LanguageManager.getInstance();

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label errorLabel;

    public LoginView() {
        this.usernameField = new TextField();
        this.passwordField = new PasswordField();
        this.loginButton = new Button(lang.get("login.loginButton"));
        this.errorLabel = new Label();

        Stylesheets.addViewStylesheet(this, "login.css");
    }

    public void show() {
        this.getStyleClass().add("login-view");
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);

        usernameField.setPrefWidth(400);
        passwordField.setPrefWidth(400);
        loginButton.setPrefWidth(250);
        loginButton.setDefaultButton(true);

        errorLabel.getStyleClass().add("login-error-text");
        errorLabel.setOpacity(0);
        errorLabel.setMinHeight(20);
        errorLabel.setWrapText(true);
        errorLabel.setPrefWidth(300);
        errorLabel.setPrefHeight(30);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        errorLabel.setOnMouseClicked(e -> hideError());

        StackPane usernameStack = createFloatingLabelField(usernameField, lang.get("login.usernameLabel"));
        StackPane passwordStack = createFloatingLabelField(passwordField, lang.get("login.passwordLabel"));

        VBox panel = new VBox();
        panel.getStyleClass().add("login-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        panel.getChildren().addAll(usernameStack, passwordStack, loginButton, errorLabel);

        getChildren().add(panel);

        this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            javafx.scene.Node target = (javafx.scene.Node) event.getTarget();
            boolean clickedOnField = false;

            while (target != null) {
                if (target == usernameField || target == passwordField) {
                    clickedOnField = true;
                    break;
                }
                target = target.getParent();
            }

            if (!clickedOnField) {
                Platform.runLater(this::requestFocus);
            }
        });
        Platform.runLater(this::requestFocus);
    }

    private StackPane createFloatingLabelField(TextField field, String labelText) {
        Label floatingLabel = new Label(labelText);
        floatingLabel.getStyleClass().add("floating-label");

        StackPane stack = new StackPane(field, floatingLabel);
        stack.setAlignment(Pos.CENTER_LEFT);
        stack.setPrefWidth(400);
        stack.setMaxWidth(400);

        floatingLabel.setMouseTransparent(true);
        floatingLabel.setTranslateX(10);
        floatingLabel.setTranslateY(0);

        // Define the animation to move the label up when the field gains focus
        TranslateTransition moveUp = new TranslateTransition(Duration.millis(150), floatingLabel);
        moveUp.setToY(-22);

        // Define the animation to shrink the label slightly when it moves up
        ScaleTransition shrink = new ScaleTransition(Duration.millis(150), floatingLabel);
        shrink.setToX(0.8);
        shrink.setToY(0.8);

        // Define the animation to move the label back down when the field loses focus
        TranslateTransition moveDown = new TranslateTransition(Duration.millis(150), floatingLabel);
        moveDown.setToY(0);

        // Define the animation to grow the label back to its original size
        ScaleTransition grow = new ScaleTransition(Duration.millis(150), floatingLabel);
        grow.setToX(1.0);
        grow.setToY(1.0);

        // Trigger animations based on focus changes
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Field gained focus: move label up and shrink it
                moveUp.playFromStart();
                shrink.playFromStart();
                floatingLabel.getStyleClass().add("floating-label-active");
            } else {
                // Field lost focus: if empty, move label back down
                if (field.getText().isEmpty()) {
                    moveDown.playFromStart();
                    grow.playFromStart();
                    floatingLabel.getStyleClass().remove("floating-label-active");
                }
            }
        });

        // Reset label position if the field is cleared while not focused
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (field.getText().isEmpty() && !field.isFocused()) {
                moveDown.playFromStart();
                grow.playFromStart();
                floatingLabel.getStyleClass().remove("floating-label-active");
            }
        });

        return stack;
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

    public void showError(String messageText) {
        errorLabel.setText(messageText);
        errorLabel.setOpacity(1);
    }

    public void hideError() {
        errorLabel.setOpacity(0);
    }
}