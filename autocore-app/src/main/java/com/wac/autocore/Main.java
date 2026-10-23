package com.wac.autocore;

import com.wac.autocore.view.AutoCoreApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * <b>Main</b>
 * <p>Responsibility: Entry point of the application. Chooses the mode before Spring starts:</p>
 * <ul>
 *   <li><b>UI</b> (default): starts JavaFX via {@link AutoCoreApp}.</li>
 *   <li><b>Headless</b> ({@code --spring.profiles.active=headless}): starts only the Spring context,
 *       runs the scenario in {@code HeadlessRunner} and exits the process.</li>
 * </ul>
 * <p>The profile is checked here and not by Spring, because the choice must be made before the context
 * exists. It can be given as a program argument, system property or environment variable.</p>
 * <p>{@code AutoCoreApp} is referenced only in the UI branch. In headless mode it is therefore
 * never loaded, and JavaFX is not needed to run the application.</p>
 */
public class Main {

    private static final String PROFILE_ARG = "--spring.profiles.active=";
    private static final String HEADLESS = "headless";
    private static final java.util.logging.Logger JAVAFX_LOGGER =
            java.util.logging.Logger.getLogger("javafx");

    public static void main(String[] args) {
        if (isHeadless(args)) {
            runHeadless(args);
        } else {
            JAVAFX_LOGGER.setFilter(rec -> rec.getMessage() == null
                    || !rec.getMessage().startsWith("Unsupported JavaFX configuration"));
            AutoCoreApp.main(args);
        }
    }

    private static void runHeadless(String[] args) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(AutoCoreConfig.class)
                .web(WebApplicationType.NONE)
                .headless(true)
                .run(args);
        System.exit(SpringApplication.exit(ctx));
    }

    private static boolean isHeadless(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(PROFILE_ARG) && containsHeadless(arg.substring(PROFILE_ARG.length()))) {
                return true;
            }
        }
        return containsHeadless(System.getProperty("spring.profiles.active"))
                || containsHeadless(System.getenv("SPRING_PROFILES_ACTIVE"));
    }

    // Handles null and comma-separated lists, e.g. "test,headless"
    private static boolean containsHeadless(String profiles) {
        if (profiles == null) {
            return false;
        }
        for (String profile : profiles.split(",")) {
            if (HEADLESS.equals(profile.trim())) {
                return true;
            }
        }
        return false;
    }
}
