package com.wac.autocore.i18n;

import java.util.Locale;

/**
 * <b>Translator</b>
 * <p>Responsibility: Translates language keys to text in the active language. The contract for all
 * translation in the application, without a dependency on JavaFX.</p>
 * <p>A missing key gives {@code !key!} instead of an exception, so a forgotten
 * language entry shows up in the UI without crashing it.</p>
 */
public interface Translator {

    String get(String key, Object... args);
    Locale getLocale();
    void setLocale(Locale locale);
}
