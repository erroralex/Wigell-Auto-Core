package com.wac.autocore.view;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.PaymentService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <b>AutoCoreApp</b>
 * <p>Ansvar: Startar Spring-kontexten och JavaFX-applikationen, skapar huvudfönstret och kopplar in CSS-stilmallen.
 * Stänger Spring-kontexten (och därmed databasanslutningen) när applikationen avslutas.</p>
 */
public class AutoCoreApp extends Application {

    private static Stage primaryStage;
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private static final double MIN_ZOOM = 0.7;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.05;

    private static final double BASE_FONT_SIZE = 13;
    private double currentZoom = 1.0;

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        Thread.currentThread().setContextClassLoader(AutoCoreApp.class.getClassLoader());

        springContext = new SpringApplicationBuilder(AutoCoreConfig.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            AutoCoreApp.primaryStage = primaryStage;

            GarageSystem garageSystem = springContext.getBean(GarageSystem.class);
            InvoiceService invoiceService = springContext.getBean(InvoiceService.class);
            PaymentService paymentService = springContext.getBean(PaymentService.class);
            BookingService bookingService = springContext.getBean(BookingService.class);

            MainLayout layout = new MainLayout(
                    garageSystem,
                    invoiceService,
                    paymentService,
                    bookingService
            );

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

            // Fönstertiteln binds, annars ligger den kvar på svenska efter språkbyte
            primaryStage.titleProperty().bind(LanguageManager.getInstance().bind("app.title"));

            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResource("assets/wigell_auto_icon.png").toExternalForm()));
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Körs när fönstret stängs. Stänger Spring-kontexten så att databasanslutningen släpps.
    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
