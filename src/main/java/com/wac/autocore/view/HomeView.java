package com.wac.autocore.view;

import com.wac.autocore.service.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;

/**
 * <b>HomeView</b>
 * <p>Ansvar: Startvy som visar logotyp och välkomsttext.</p>
 */
public class HomeView extends VBox {

    private static final String LOGO_PATH = "assets/wigell-auto-logo.png";
    private final LanguageManager lang = LanguageManager.getInstance();

    public HomeView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);

        Label welcomeLabel = new Label();
        welcomeLabel.textProperty().bind(lang.bind("home.welcome"));
        welcomeLabel.getStyleClass().add("text-content");
        welcomeLabel.setWrapText(true);
        welcomeLabel.setMaxWidth(900);

        this.getChildren().addAll(createLogo(), welcomeLabel);
    }

    // Visar logotypen, eller appens namn som text om bilden inte kan laddas
    private Label createLogo() {
        Label logo = new Label();
        URL logoUrl = getClass().getResource(LOGO_PATH);

        if (logoUrl != null) {
            ImageView imageView = new ImageView(new Image(logoUrl.toExternalForm()));
            imageView.setFitWidth(800);
            imageView.setPreserveRatio(true);
            logo.setGraphic(imageView);
            logo.setAlignment(Pos.TOP_CENTER);
        } else {
            System.err.println("Could not load " + LOGO_PATH + ". Using text placeholder.");
            logo.textProperty().bind(lang.bind("app.title"));
        }
        return logo;
    }
}
