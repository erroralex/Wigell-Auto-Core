package com.wac.autocore.view.i18n;

import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.Locale;
import java.util.Objects;

import static com.wac.autocore.i18n.ResourceBundleTranslator.ENGLISH;
import static com.wac.autocore.i18n.ResourceBundleTranslator.SWEDISH;

/**
 * <b>LanguageManager</b>
 * <p>Responsibility: <b>Adapter</b> that makes a {@link Translator} usable in JavaFX. The translation
 * is handled by the wrapped translator; this class adds what the views need:
 * bindings that update on language switch ({@link #bind}) and an observable
 * language property ({@link #localeProperty()}).</p>
 * <p>All language switches in the UI must go through this class, otherwise the
 * bindings are not updated. That is why the wrapped translator is not exposed.</p>
 * <p>Implements {@link Translator} itself, so the UI can pass it to code
 * that only knows the interface, e.g. the error formatting.</p>
 */
public final class LanguageManager implements Translator {

    private static final LanguageManager INSTANCE =
            new LanguageManager(new ResourceBundleTranslator(SWEDISH));

    private final Translator translator;
    private final ReadOnlyObjectWrapper<Locale> locale = new ReadOnlyObjectWrapper<>();

    LanguageManager(Translator translator) {
        this.translator = Objects.requireNonNull(translator, "translator");
        setLocale(translator.getLocale());
    }

    public static LanguageManager getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key, Object... args) {
        return translator.get(key, args);
    }

    // The translator is the single source of the active language
    @Override
    public Locale getLocale() {
        return translator.getLocale();
    }

    @Override
    public void setLocale(Locale newLocale) {
        translator.setLocale(newLocale);
        Locale.setDefault(newLocale);
        locale.set(newLocale);
    }

    // Toggles between Swedish and English.
    public void toggleLanguage() {
        setLocale(SWEDISH.getLanguage().equals(getLocale().getLanguage()) ? ENGLISH : SWEDISH);
    }

    // Exposes the active language to listeners, e.g. views that are rebuilt on language switch.
    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale.getReadOnlyProperty();
    }

    // Gives a text that updates automatically on language switch.
    public StringBinding bind(String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), locale);
    }
}
