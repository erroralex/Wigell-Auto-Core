package com.wac.autocore.view;

import com.wac.autocore.view.component.BrandMark;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.Stylesheets;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * <b>LoginView</b>
 * <p>Responsibility: The login screen. A dark brand panel on the left and the login form on the right.
 * All sizes, spacing and colors come from login.css and theme.css.</p>
 */
public class LoginView extends HBox {

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
        getStyleClass().add("login-view");

        VBox brandPanel = createBrandPanel();
        StackPane formPanel = createFormPanel();

        HBox.setHgrow(brandPanel, Priority.ALWAYS);
        HBox.setHgrow(formPanel, Priority.ALWAYS);
        getChildren().addAll(brandPanel, formPanel);

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());

        Platform.runLater(usernameField::requestFocus);
    }

    private VBox createBrandPanel() {
        BrandMark brandHeader = new BrandMark(lang.get("app.brand"));

        Label headline = new Label(lang.get("login.headline"));
        headline.getStyleClass().add("login-headline");

        Label tagline = new Label(lang.get("login.tagline"));
        tagline.getStyleClass().add("login-tagline");

        VBox hero = new VBox(headline, tagline);
        hero.getStyleClass().add("login-hero");

        Region topSpacer = new Region();
        Region bottomSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox panel = new VBox(brandHeader, topSpacer, hero, bottomSpacer);
        panel.getStyleClass().add("login-brand");
        return panel;
    }

    private StackPane createFormPanel() {
        Label title = new Label(lang.get("login.title"));
        title.getStyleClass().add("login-title");

        Label subtitle = new Label(lang.get("login.subtitle"));
        subtitle.getStyleClass().add("text-secondary");

        VBox intro = new VBox(title, subtitle);
        intro.getStyleClass().add("login-intro");

        loginButton.setDefaultButton(true);
        loginButton.getStyleClass().addAll("btn", "btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        errorLabel.getStyleClass().add("login-error-text");
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setOpacity(0);
        errorLabel.setOnMouseClicked(e -> hideError());

        VBox form = new VBox(
                intro,
                createField(lang.get("login.usernameLabel"), usernameField),
                createField(lang.get("login.passwordLabel"), passwordField),
                loginButton,
                errorLabel
        );
        form.getStyleClass().add("login-form");
        form.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane panel = new StackPane(form);
        panel.getStyleClass().add("login-form-panel");
        return panel;
    }

    private VBox createField(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("login-field-label");
        label.setLabelFor(field);

        VBox group = new VBox(label, field);
        group.getStyleClass().add("login-field");
        return group;
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
