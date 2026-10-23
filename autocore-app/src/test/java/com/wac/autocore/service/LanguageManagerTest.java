package com.wac.autocore.service;

import com.wac.autocore.model.value.Money;
import com.wac.autocore.view.i18n.LanguageManager;
import javafx.beans.binding.StringBinding;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>LanguageManagerTest</b>
 * <p>Responsibility: Tests language switching, lookup, formatting and bindings in {@link LanguageManager}.</p>
 * <p>JavaFX properties and bindings work without starting the JavaFX platform,
 * so the tests require no window.</p>
 */
class LanguageManagerTest {

    private static final Locale SWEDISH = new Locale("sv", "SE");

    private static Locale originalDefault;

    private final LanguageManager lang = LanguageManager.getInstance();

    @BeforeAll
    static void saveDefaultLocale() {
        // LanguageManager may change the JVM's default locale; it is restored after the tests
        originalDefault = Locale.getDefault();
    }

    @AfterAll
    static void restoreDefaultLocale() {
        LanguageManager.getInstance().setLocale(SWEDISH);
        Locale.setDefault(originalDefault);
    }

    @BeforeEach
    void resetToSwedish() {
        // Singleton: each test starts from the same state regardless of execution order
        lang.setLocale(SWEDISH);
    }

    @Test
    void returnsSwedishText() {
        assertEquals("Hem", lang.get("nav.home"));
        assertEquals("Fältet Namn måste fyllas i.", lang.get("error.requiredField", "Namn"));
    }

    @Test
    void toggleSwitchesBetweenSwedishAndEnglish() {
        lang.toggleLanguage();
        assertEquals("en", lang.getLocale().getLanguage());
        assertEquals("Home", lang.get("nav.home"));

        lang.toggleLanguage();
        assertEquals("sv", lang.getLocale().getLanguage());
        assertEquals("Hem", lang.get("nav.home"));
    }

    @Test
    void missingKeyIsVisibleInsteadOfCrashing() {
        assertEquals("!does.not.exist!", lang.get("does.not.exist"));
    }

    @Test
    void textWithoutArgumentsKeepsApostrophe() {
        // Without arguments MessageFormat is not run, so a single apostrophe is ok
        lang.toggleLanguage();
        assertEquals(true, lang.get("home.welcome").contains("Wigell Auto's"));
    }

    @Test
    void priceFollowsLanguage() {
        assertEquals("1097,50 kr", lang.get("format.price", Money.of("1097.50")));

        lang.toggleLanguage();
        assertEquals("1097.50 kr", lang.get("format.price", Money.of("1097.50")));
    }

    @Test
    void moneyArgumentIsFormattedLikeANumber() {
        assertEquals("1097,50 kr", lang.get("format.price", Money.of("1097.50")));

        lang.toggleLanguage();
        assertEquals("1097.50 kr", lang.get("format.price", Money.of("1097.50")));
    }

    @Test
    void idsHaveNoThousandsSeparator() {
        assertEquals("Faktura #1234", lang.get("payment.invoice", 1234));
        assertEquals("Okänd (#12345)", lang.get("common.unknownId", 12345));
    }

    @Test
    void bindingUpdatesWhenLanguageChanges() {
        StringBinding binding = lang.bind("nav.home");
        assertEquals("Hem", binding.get());

        lang.toggleLanguage();
        assertEquals("Home", binding.get());
    }

    @Test
    void localeListenerIsNotifiedOnToggle() {
        // MainLayout rebuilds the view via this listener
        AtomicInteger calls = new AtomicInteger();
        lang.localeProperty().addListener((obs, oldLocale, newLocale) -> calls.incrementAndGet());

        lang.toggleLanguage();
        lang.toggleLanguage();

        assertEquals(2, calls.get());
    }
}
