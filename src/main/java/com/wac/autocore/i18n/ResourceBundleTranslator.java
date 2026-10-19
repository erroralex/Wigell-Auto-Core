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
 * <p>Ansvar: Slår upp översatta texter i språkfilerna {@code i18n/messages*.properties}
 * (UTF-8) och formaterar argument, där {@code Money} alltid formateras via {@link MoneyFormat}.</p>
 * <p>Innehåller ingen JavaFX-kod och kan därför användas av både gränssnittet och headless-läget.
 * Gränssnittet använder den via {@code LanguageManager}, som lägger till JavaFX-bindningar.</p>
 * <p>Ändrar medvetet inte {@code Locale.setDefault}: varje instans har sitt eget språk,
 * så att headless-körningar och tester inte påverkar varandra.</p>
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

    // Byter språk för kommande uppslag.
    @Override
    public void setLocale(Locale newLocale) {
        Objects.requireNonNull(newLocale, "locale");
        this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale, UTF8_CONTROL);
        this.locale = newLocale;
    }

    // Belopp formateras av MoneyFormat (enda källan), så bundlarna använder bara {0} för Money
    private Object[] toFormatArgs(Object[] args) {
        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            converted[i] = args[i] instanceof Money ? MoneyFormat.format((Money) args[i], getLocale()) : args[i];
        }
        return converted;
    }
}
