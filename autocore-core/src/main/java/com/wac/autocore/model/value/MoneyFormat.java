package com.wac.autocore.model.value;

import com.wac.autocore.exception.ValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

// Används av Dialogerna för att formatera Money-värden. Vet inget om databasen eller Hibernate.
// Formaterar och tolkar belopp för visning och inmatning utan JavaFX.
// Locale, komma och språknycklar hör till användargränssnittet.
// De ska inte påverka vad som sparas i en REAL-kolumn.
public final class MoneyFormat {

    private MoneyFormat() {}

    private static final Pattern INPUT = Pattern.compile("-?(\\d+([.,]\\d{1,2})?|[.,]\\d{1,2})");

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

    // Accepterar både komma och punkt, med högst två decimaler. Ogiltig eller tom text ger ett valideringsfel.
    // Fler decimaler och exponentskrivning avvisas, så att t.ex. 1,234 inte tyst blir 1,23.
    public static Money parse(String text) {
        if (text == null || !INPUT.matcher(text.trim()).matches()) {
            throw new ValidationException("error.invalidAmount");
        }
        return Money.of(text);
    }
}