package com.wac.autocore.model.value;

import com.wac.autocore.exception.ValidationException;

import java.util.Locale;

// Används av Dialogerna för att formatera Money-värden. Vet inget om databasen eller Hibernate.
// Formaterar och tolkar belopp för visning och inmatning utan JavaFX.
// Locale, komma och språknycklar hör till användargränssnittet.
// De ska inte påverka vad som sparas i en REAL-kolumn.
public final class MoneyFormat {

    private MoneyFormat() {}

    // PLAIN: 1097,50 (kan tolkas tillbaka av parse, t.ex. inmatningsfält).
    // GROUPED: 1 097,50 (med tusentalsavgränsare, för visning i tabeller).
    public enum Style {
        PLAIN,
        GROUPED
    }

    // Alltid två decimaler, med decimaltecken enligt locale (komma på svenska).
    // Det är den här metoden som avgör hur ett belopp ser ut.
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