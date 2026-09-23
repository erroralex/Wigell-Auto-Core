package com.wac.autocore.service;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * <b>LanguageManager</b>
 * <p>Ansvar: Laddar och växlar språk (svenska/engelska) för gränssnittets texter.</p>
 */
public final class LanguageManager {

    private static final String BUNDLE_NAME = "i18n/messages";
    private static final Locale SWEDISH = new Locale("sv", "SE");
    private static final Locale ENGLISH = new Locale("en", "US");

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
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload) throws IOException {
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

    private static final LanguageManager INSTANCE = new LanguageManager();

    private final ReadOnlyObjectWrapper<Locale> locale = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<ResourceBundle> bundle = new ReadOnlyObjectWrapper<>();

    private LanguageManager() {
        setLocale(SWEDISH);
    }

    public static LanguageManager getInstance() {
        return INSTANCE;
    }

    /** Ansvar: Byter aktivt språk och uppdaterar alla bundna texter. */
    public void setLocale(Locale newLocale) {
        bundle.set(ResourceBundle.getBundle(BUNDLE_NAME, newLocale, UTF8_CONTROL));
        locale.set(newLocale);
    }

    /** Ansvar: Växlar mellan svenska och engelska. */
    public void toggleLanguage() {
        setLocale(SWEDISH.getLanguage().equals(getLocale().getLanguage()) ? ENGLISH : SWEDISH);
    }

    public Locale getLocale() {
        return locale.get();
    }

    /** Ansvar: Exponerar aktivt språk för lyssnare, t.ex. vyer som byggs om vid språkbyte. */
    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale.getReadOnlyProperty();
    }

    /** Ansvar: Hämtar en översatt text för given nyckel. */
    public String get(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.get().getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
        return args.length == 0 ? pattern : new MessageFormat(pattern, getLocale()).format(args);
    }

    /** Ansvar: Ger en text som uppdateras automatiskt vid språkbyte. */
    public StringBinding bind(String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), bundle);
    }
}
