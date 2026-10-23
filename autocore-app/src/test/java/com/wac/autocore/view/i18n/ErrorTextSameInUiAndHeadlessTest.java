package com.wac.autocore.view.i18n;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.i18n.ErrorReportFormatter;
import com.wac.autocore.i18n.LocalizedError;
import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.model.WorkOrderStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>ErrorTextSameInUiAndHeadlessTest</b>
 * <p>Responsibility: Proves that the same error gives exactly the same text in the UI and in headless mode.
 * The UI translates via {@link LanguageManager} (the JavaFX adapter), headless via a
 * standalone {@link ResourceBundleTranslator}. Both go through the same error chain and the same formatter.</p>
 */
class ErrorTextSameInUiAndHeadlessTest {

    private static Locale originalDefault;

    @BeforeAll
    static void saveDefaultLocale() {
        // LanguageManager changes the JVM's default locale; it is restored after the test
        originalDefault = Locale.getDefault();
    }

    @AfterAll
    static void restoreDefaultLocale() {
        LanguageManager.getInstance().setLocale(ResourceBundleTranslator.SWEDISH);
        Locale.setDefault(originalDefault);
    }

    @Test
    @DisplayName("Illegal status transition gives the same text in UI and headless, in both languages")
    void sameTextInUiAndHeadless() {
        ErrorReport report = ErrorHandlerChain.createDefault().resolve(
                new IllegalStatusTransitionException(WorkOrderStatus.CANCELLED, WorkOrderStatus.IN_PROGRESS));

        for (Locale locale : new Locale[]{ResourceBundleTranslator.SWEDISH, ResourceBundleTranslator.ENGLISH}) {
            LanguageManager ui = LanguageManager.getInstance();
            ui.setLocale(locale);

            LocalizedError fromUi = new ErrorReportFormatter(ui).format(report);
            LocalizedError fromHeadless = new ErrorReportFormatter(new ResourceBundleTranslator(locale)).format(report);

            assertEquals(fromHeadless, fromUi, "Language: " + locale);
        }
    }
}
