package com.wac.autocore.view;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.MechanicService;
import com.wac.autocore.service.PaymentService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.ErrorFacade;
import com.wac.autocore.view.util.GlobalExceptionHandler;
import com.wac.autocore.view.util.Stylesheets;
import com.wac.autocore.view.util.ZoomManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>AutoCoreApp</b>
 * <p>Responsibility: Starts the Spring context and the JavaFX application, creates the main window and plugs in the CSS stylesheet.
 * Closes the Spring context (and thereby the database connection) when the application exits.</p>
 */
public class AutoCoreApp extends Application {

    private static Stage primaryStage;
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private static final LanguageManager lang = LanguageManager.getInstance();

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
        GlobalExceptionHandler.install();

        AutoCoreApp.primaryStage = primaryStage;

        Scene scene = new Scene(createLoginView(), WIDTH, HEIGHT);
        Stylesheets.applyTo(scene);

        ZoomManager.getInstance().install(scene);

        primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResource("assets/wigell_auto_icon.png").toExternalForm()));

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.titleProperty().bind(LanguageManager.getInstance().bind("app.title"));
        primaryStage.show();
    }

    // A fresh login view, used at start-up and after logging out
    private LoginView createLoginView() {
        LoginView loginView = new LoginView();
        loginView.show();
        loginView.getLoginButton().setOnAction(e -> handleLogin(loginView));
        return loginView;
    }

    private void handleLogin(LoginView loginView) {
        try {
            AuthenticationManager authManager = springContext.getBean(AuthenticationManager.class);
            var auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginView.getUsername(),
                    loginView.getPassword()
            ));

            SecurityContextHolder.getContext().setAuthentication(auth);
            showMainLayout();

        } catch (AuthenticationException ex) {
            loginView.showError(lang.get("error.login.invalidUsernameOrPassword"));
        }
    }

    private void showMainLayout() {
        try {
            Scene currentScene = primaryStage.getScene();

            InvoiceService invoiceService = springContext.getBean(InvoiceService.class);
            PaymentService paymentService = springContext.getBean(PaymentService.class);
            BookingService bookingService = springContext.getBean(BookingService.class);
            CustomerService customerService = springContext.getBean(CustomerService.class);
            VehicleService vehicleService = springContext.getBean(VehicleService.class);
            WorkOrderService workOrderService = springContext.getBean(WorkOrderService.class);
            ServiceItemService serviceItemService = springContext.getBean(ServiceItemService.class);
            MechanicService mechanicService = springContext.getBean(MechanicService.class);
            ServicePackageService servicePackageService = springContext.getBean(ServicePackageService.class);

            MainLayout layout = new MainLayout(
                    invoiceService,
                    paymentService,
                    bookingService,
                    customerService,
                    vehicleService,
                    workOrderService,
                    serviceItemService,
                    mechanicService,
                    servicePackageService
            );

            currentScene.setRoot(layout);

            // The window title is bound, otherwise it stays in Swedish after a language switch
            primaryStage.titleProperty().bind(LanguageManager.getInstance().bind("app.title"));
            primaryStage.setMaximized(true);

        } catch (Exception e) {
            ErrorFacade.handle(e);
        }
    }

    // Runs when the window is closed. Closes the Spring context so the database connection is released.
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
