package com.wac.autocore.view;

import com.wac.autocore.view.component.BrandMark;
import com.wac.autocore.view.component.Gear;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.Stylesheets;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

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

        StackPane brandPanel = createBrandPanel();
        StackPane formPanel = createFormPanel();

        HBox.setHgrow(brandPanel, Priority.ALWAYS);
        HBox.setHgrow(formPanel, Priority.ALWAYS);
        getChildren().addAll(brandPanel, formPanel);

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());

        // Nothing focused at start, so both labels begin inside their fields.
        // A click outside the fields moves focus to the view, so an empty field's label drops back.
        setFocusTraversable(true);
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!isInside(event.getTarget(), usernameField) && !isInside(event.getTarget(), passwordField)) {
                requestFocus();
            }
        });
        Platform.runLater(this::requestFocus);
    }

    // Left half. Large outline gears behind the brand mark, headline and tagline
    private StackPane createBrandPanel() {
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

        VBox content = new VBox(brandHeader, topSpacer, hero, bottomSpacer);
        content.getStyleClass().add("login-brand-content");

        StackPane panel = new StackPane(createGears(), content);
        panel.getStyleClass().add("login-brand");

        // The gears reach past the panel's edges; the clip cuts them off at the panel instead of
        // letting them draw over the form half
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(panel.widthProperty());
        clip.heightProperty().bind(panel.heightProperty());
        panel.setClip(clip);

        return panel;
    }

    // A large pair of meshing gears in the bottom right corner, the small gear up to the left
    private HBox createGears() {
        HBox gears = new HBox(new Gear("gear-login"));
        gears.getStyleClass().add("login-gears");
        gears.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(gears, Pos.BOTTOM_RIGHT);
        return gears;
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
                createFloatingLabelField(lang.get("login.usernameLabel"), usernameField),
                createFloatingLabelField(lang.get("login.passwordLabel"), passwordField),
                loginButton,
                errorLabel
        );
        form.getStyleClass().add("login-form");
        form.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane panel = new StackPane(form);
        panel.getStyleClass().add("login-form-panel");
        return panel;
    }

    private StackPane createFloatingLabelField(String labelText, TextField field) {
        Label floatingLabel = new Label(labelText);
        floatingLabel.getStyleClass().add("floating-label");
        floatingLabel.setLabelFor(field);

        StackPane stack = new StackPane(field, floatingLabel);
        stack.setAlignment(Pos.CENTER_LEFT);

        floatingLabel.setMouseTransparent(true);
        // Follows the field's padding instead of a fixed 10px, so the label lines up with the text at any zoom
        floatingLabel.translateXProperty().bind(Bindings.createDoubleBinding(
                () -> field.getPadding().getLeft() - floatingLabel.getPadding().getLeft(),
                field.paddingProperty(), floatingLabel.paddingProperty()));
        floatingLabel.setTranslateY(0);

        // Define the animation to move the label up when the field gains focus
        TranslateTransition moveUp = new TranslateTransition(Duration.millis(150), floatingLabel);

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
                // Half the field's height instead of a fixed -22px, so the label lands on the top border at any zoom
                moveUp.setToY(-field.getHeight() / 2);
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

    // True when the click target is the field itself or something inside it
    private static boolean isInside(Object target, Node field) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            if (node == field) {
                return true;
            }
            node = node.getParent();
        }
        return false;
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
