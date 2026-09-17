package com.wac.autocore.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

/**
 * <b>AutoCoreApp</b>
 * <p>Ansvar: Startar JavaFX-applikationen, skapar huvudfönstret och kopplar in CSS-stilmallen.</p>
 */
public class AutoCoreApp extends Application {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private static final double MIN_ZOOM = 0.7;
    private static final double MAX_ZOOM = 2.0;
    private static final double ZOOM_STEP = 1.05;

    private static final double BASE_FONT_SIZE = 13;
    private double currentZoom = 1.0;


    @Override
    public void start(Stage primaryStage) {
        try {
            MainLayout layout = new MainLayout();
            Scene scene = new Scene(layout, WIDTH, HEIGHT);
            scene.setFill(javafx.scene.paint.Color.web("#212121"));

            // Laddar stilmall från samma paket via classpath
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            // UI Ctrl-scroll Zoom
            scene.addEventFilter(ScrollEvent.SCROLL, event -> {
                if (event.isControlDown()) {
                    double factor = event.getDeltaY() > 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
                    currentZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom * factor));

                    layout.setStyle("-fx-font-size: " + (BASE_FONT_SIZE * currentZoom) + "px;");
                    event.consume();
                }
            });

            primaryStage.setTitle("Wigell Auto");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResource("assets/wigell_auto_icon.png").toExternalForm()));
            primaryStage.show();

        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
