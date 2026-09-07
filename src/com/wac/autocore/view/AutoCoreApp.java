package com.wac.autocore.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * <b>AutoCoreApp</b>
 * <p>Ansvar: Startar JavaFX-applikationen, skapar huvudfönstret och kopplar in CSS-stilmallen.</p>
 */
public class AutoCoreApp extends Application {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    @Override
    public void start(Stage primaryStage) {
        try {
            MainLayout layout = new MainLayout();
            Scene scene = new Scene(layout, WIDTH, HEIGHT);

            // Laddar stilmall från samma paket via classpath
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setTitle("Wigell Auto");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
