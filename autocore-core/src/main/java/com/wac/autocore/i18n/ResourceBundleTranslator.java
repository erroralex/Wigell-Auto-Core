package com.wac.autocore.i18n;

import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.MoneyFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

/**
 * <b>ResourceBundleTranslator</b>
 * <p>Responsibility: Looks up translated texts in the language files {@code i18n/messages*.properties}
 * (UTF-8) and formats arguments, where {@code Money} is always formatted via {@link MoneyFormat}.</p>
 * <p>Contains no JavaFX code and can therefore be used by both the UI and headless mode.
 * The UI uses it via {@code LanguageManager}, which adds JavaFX bindings.</p>
 * <p>Deliberately does not change {@code Locale.setDefault}: each instance has its own language,
 * so headless runs and tests do not affect each other.</p>
 */
public final class ResourceBundleTranslator implements Translator {

    private static final String BUNDLE_NAME = "i18n/messages";
    public static final Locale SWEDISH = new Locale("sv", "SE");
    public static final Locale ENGLISH = new Locale("en", "US");

    private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {

        @Override
        public List<String> getFormats(String baseName) {
            return FORMAT_PROPERTIES;
        }

        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IOException {
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        }
    };

    private Locale locale;
    private ResourceBundle bundle;

    public ResourceBundleTranslator(Locale initial) {
        setLocale(initial);
    }

    @Override
    public String get(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
        return args.length == 0 ? pattern : new MessageFormat(pattern, getLocale()).format(toFormatArgs(args));
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    // Switches language for upcoming lookups.
    @Override
    public void setLocale(Locale newLocale) {
        Objects.requireNonNull(newLocale, "locale");
        this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale, UTF8_CONTROL);
        this.locale = newLocale;
    }

    // Amounts are formatted by MoneyFormat (the single source), so the bundles only use {0} for Money
    private Object[] toFormatArgs(Object[] args) {
        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            converted[i] = args[i] instanceof Money ? MoneyFormat.format((Money) args[i], getLocale()) : args[i];
        }
        return converted;
    }
}
