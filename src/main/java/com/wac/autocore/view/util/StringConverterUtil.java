package com.wac.autocore.view.util;

import javafx.util.StringConverter;

import java.util.function.Function;

//Små hjälpmetoder för JavaFX-omvandlare som används i ComboBox.
public final class StringConverterUtil {

    private StringConverterUtil() {
    }

    // Enbart visning; ComboBox är inte redigerbar, så fromString behövs inte.
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
