package com.wac.autocore.service;

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
 * <p>Ansvar: Testar språkväxling, uppslag, formatering och bindningar i {@link LanguageManager}.</p>
 * <p>JavaFX-properties och bindningar fungerar utan att JavaFX-plattformen startas,
 * så testerna kräver inget fönster.</p>
 */
class LanguageManagerTest {

    private static final Locale SWEDISH = new Locale("sv", "SE");

    private static Locale originalDefault;

    private final LanguageManager lang = LanguageManager.getInstance();

    @BeforeAll
    static void saveDefaultLocale() {
        // LanguageManager kan ändra JVM:ens standardspråk; återställs efter testerna
        originalDefault = Locale.getDefault();
    }

    @AfterAll
    static void restoreDefaultLocale() {
        LanguageManager.getInstance().setLocale(SWEDISH);
        Locale.setDefault(originalDefault);
    }

    @BeforeEach
    void resetToSwedish() {
        // Singleton: varje test börjar från samma läge oavsett körordning
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
        // Utan argument körs inte MessageFormat, så en enkel apostrof är ok
        lang.toggleLanguage();
        assertEquals(true, lang.get("home.welcome").contains("Wigell Auto's"));
    }

    @Test
    void priceFollowsLanguage() {
        assertEquals("1097,50 kr", lang.get("format.price", 1097.5));

        lang.toggleLanguage();
        assertEquals("1097.50 kr", lang.get("format.price", 1097.5));
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
        // MainLayout bygger om vyn via denna lyssnare
        AtomicInteger calls = new AtomicInteger();
        lang.localeProperty().addListener((obs, oldLocale, newLocale) -> calls.incrementAndGet());

        lang.toggleLanguage();
        lang.toggleLanguage();

        assertEquals(2, calls.get());
    }
}
