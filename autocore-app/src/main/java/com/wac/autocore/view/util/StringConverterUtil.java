package com.wac.autocore.view.util;

import javafx.util.StringConverter;

import java.util.function.Function;

//Small helper methods for JavaFX converters used in ComboBox.
public final class StringConverterUtil {

    private StringConverterUtil() {
    }

    // Display only; the ComboBox is not editable, so fromString is not needed.
    public static <T> StringConverter<T> display(Function<T, String> toText) {
        return new StringConverter<T>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }

}
