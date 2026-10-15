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
 * <p>Ansvar: <b>Adapter</b> som gör en {@link Translator} användbar i JavaFX. Översättningen
 * sköts av den inlindade översättaren; den här klassen lägger till det vyerna behöver:
 * bindningar som uppdateras vid språkbyte ({@link #bind}) och en observerbar
 * språk-property ({@link #localeProperty()}).</p>
 * <p>Alla språkbyten i gränssnittet måste gå via den här klassen, annars uppdateras inte
 * bindningarna. Därför exponeras inte den inlindade översättaren.</p>
 * <p>Implementerar själv {@link Translator}, så att gränssnittet kan skicka den till kod
 * som bara känner till interfacet, t.ex. felformateringen.</p>
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

    // Översättaren är enda källan för aktivt språk
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

    // Växlar mellan svenska och engelska.
    public void toggleLanguage() {
        setLocale(SWEDISH.getLanguage().equals(getLocale().getLanguage()) ? ENGLISH : SWEDISH);
    }

    // Exponerar aktivt språk för lyssnare, t.ex. vyer som byggs om vid språkbyte.
    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale.getReadOnlyProperty();
    }

    // Ger en text som uppdateras automatiskt vid språkbyte.
    public StringBinding bind(String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), locale);
    }
}
