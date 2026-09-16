package com.wac.autocore.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class HomeView extends VBox {

    public HomeView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);

        Label logo = new Label();
        Image logoImage = null;
        try {
            logoImage = new Image(getClass().getResource("assets/wigell-auto-logo.png").toExternalForm());
        } catch (Exception e) {
            System.err.println("Error loading status image: " + "wigell-auto-logo.png" + ". Using text placeholder.");
        }

        if (logoImage != null) {
            ImageView statusImageView = new ImageView(logoImage);
            statusImageView.setFitWidth(800);
            statusImageView.setPreserveRatio(true);
            logo.setGraphic(statusImageView);
            logo.setAlignment(Pos.TOP_CENTER);
        } else {
            logo.setText("Wigell Auto");
        }

        Label welcomeLabel = new Label(
                "Welcome to Wigell Auto's service management tool. Use the menu on the left" + "\n" +
                        "to manage customers, vehicles, bookings, and work orders."
        );

        welcomeLabel.getStyleClass().add("text-title");
        welcomeLabel.setWrapText(true);
        welcomeLabel.setMaxWidth(900);

        this.getChildren().addAll(logo, welcomeLabel);

    }


}
