package com.wac.autocore.model.value;

import com.wac.autocore.exception.ValidationException;

import java.util.Locale;

// Används av Dialogerna för att formatera Money-värden. Vet inget om databasen eller Hibernate.
// Formaterar och tolkar belopp för visning och inmatning utan JavaFX.
// Locale, komma och språknycklar hör till användargränssnittet.
// De ska inte påverka vad som sparas i en REAL-kolumn.
public final class MoneyFormat {

    private MoneyFormat() {}

    // Alltid två decimaler, med decimaltecken enligt locale (komma på svenska).
    public static String format(Money money, Locale locale) {
        if (money == null) {
            return "";
        }
        return String.format(locale, "%.2f", money.toBigDecimal());
    }

    // Accepterar både komma och punkt. Ogiltig eller tom text ger ett valideringsfel.
    public static Money parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("error.invalidAmount");
        }
        try {
            return Money.of(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("error.invalidAmount");
        }
    }
}