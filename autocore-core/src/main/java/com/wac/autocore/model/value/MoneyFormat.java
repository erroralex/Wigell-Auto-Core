package com.wac.autocore.model.value;

import com.wac.autocore.exception.ValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

// Used by the dialogs to format Money values. Knows nothing about the database or Hibernate.
// Formats and parses amounts for display and input without JavaFX.
// Locale, comma and language keys belong to the user interface.
// They should not affect what is stored in a REAL column.
public final class MoneyFormat {

    private MoneyFormat() {}

    private static final Pattern INPUT = Pattern.compile("-?(\\d+([.,]\\d{1,2})?|[.,]\\d{1,2})");

    // PLAIN: 1097,50 (can be parsed back by parse, e.g. input fields).
    // GROUPED: 1 097,50 (with thousands separator, for display in tables).
    public enum Style {
        PLAIN,
        GROUPED
    }

    // Always two decimals, with the decimal separator according to the locale (comma in Swedish).
    // This is the method that decides what an amount looks like.
    public static String format(Money money, Locale locale, Style style) {
        if (money == null) {
            return "";
        }
        String pattern = style == Style.GROUPED ? "%,.2f" : "%.2f";
        return String.format(locale, pattern, money.toBigDecimal());
    }

    public static String format(Money money, Locale locale) {
        return format(money, locale, Style.PLAIN);
    }

    // Accepts both comma and period, with at most two decimals. Invalid or empty text gives a validation error.
    // More decimals and exponent notation are rejected, so that e.g. 1,234 does not silently become 1,23.
    public static Money parse(String text) {
        if (text == null || !INPUT.matcher(text.trim()).matches()) {
            throw new ValidationException("error.invalidAmount");
        }
        return Money.of(text);
    }
}