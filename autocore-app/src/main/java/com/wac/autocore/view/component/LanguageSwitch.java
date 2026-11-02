package com.wac.autocore.view.component;

import com.wac.autocore.view.i18n.LanguageManager;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.Locale;

import static com.wac.autocore.i18n.ResourceBundleTranslator.ENGLISH;
import static com.wac.autocore.i18n.ResourceBundleTranslator.SWEDISH;

/**
 * <b>LanguageSwitch</b>
 * <p>Responsibility: Segmented control for choosing the UI language, one {@link ToggleButton} per
 * language in a shared {@link ToggleGroup}. The selected option always follows the active locale in
 * {@link LanguageManager}, so every switch on screen stays in sync no matter where the language was changed.</p>
 * <p>Styled with {@code .nav-language-switch}; each option has {@code .language-option}.</p>
 */
public class LanguageSwitch extends HBox {

    private static final List<Locale> LANGUAGES = List.of(SWEDISH, ENGLISH);

    private final LanguageManager lang = LanguageManager.getInstance();
    private final ToggleGroup group = new ToggleGroup();

    private final ChangeListener<Locale> localeListener = (obs, oldLocale, newLocale) -> selectFor(newLocale);

    public LanguageSwitch() {
        getStyleClass().add("nav-language-switch");

        for (Locale locale : LANGUAGES) {
            getChildren().add(createOption(locale));
        }

        // Show the active language as selected
        selectFor(lang.getLocale());
        lang.localeProperty().addListener(new WeakChangeListener<>(localeListener));
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> onSelected(oldToggle, newToggle));
    }

    private ToggleButton createOption(Locale locale) {
        ToggleButton option = new ToggleButton(locale.getLanguage().toUpperCase(Locale.ROOT));
        option.setUserData(locale);
        option.setToggleGroup(group);
        option.getStyleClass().add("language-option");
        option.setAccessibleText(nativeName(locale));
        option.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(option, Priority.ALWAYS);
        return option;
    }

    private void onSelected(Toggle oldToggle, Toggle newToggle) {
        // A ToggleGroup lets the user deselect by clicking the selected option again.
        if (newToggle == null) {
            group.selectToggle(oldToggle);
            return;
        }
        Locale chosen = (Locale) newToggle.getUserData();
        if (!sameLanguage(chosen, lang.getLocale())) {
            lang.setLocale(chosen);
        }
    }

    private void selectFor(Locale locale) {
        for (Toggle toggle : group.getToggles()) {
            if (sameLanguage((Locale) toggle.getUserData(), locale)) {
                group.selectToggle(toggle);
                return;
            }
        }
    }

    private static boolean sameLanguage(Locale a, Locale b) {
        return a.getLanguage().equals(b.getLanguage());
    }

    // "Svenska", "English": each language named in itself, as language pickers usually do
    private static String nativeName(Locale locale) {
        String name = locale.getDisplayLanguage(locale);
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }
}
