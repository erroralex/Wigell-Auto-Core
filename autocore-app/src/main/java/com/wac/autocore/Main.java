package com.wac.autocore;

import com.wac.autocore.view.AutoCoreApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * <b>Main</b>
 * <p>Ansvar: Startpunkt för applikationen. Väljer läge innan Spring startar:</p>
 * <ul>
 *   <li><b>Gränssnitt</b> (standard): startar JavaFX via {@link AutoCoreApp}.</li>
 *   <li><b>Headless</b> ({@code --spring.profiles.active=headless}): startar bara Spring-kontexten,
 *       kör scenariot i {@code HeadlessRunner} och avslutar processen.</li>
 * </ul>
 * <p>Profilen kontrolleras här och inte av Spring, eftersom valet måste göras innan kontexten
 * finns. Den kan anges som programargument, systemegenskap eller miljövariabel.</p>
 * <p>{@code AutoCoreApp} refereras bara i gränssnittsgrenen. I headless-läget laddas den därför
 * aldrig, och JavaFX behövs inte för att köra applikationen.</p>
 */
public class Main {

    private static final String PROFILE_ARG = "--spring.profiles.active=";
    private static final String HEADLESS = "headless";

    public static void main(String[] args) {
        if (isHeadless(args)) {
            runHeadless(args);
        } else {
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

    // Klarar null och kommaseparerade listor, t.ex. "test,headless"
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
