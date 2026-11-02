package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.MoneyFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>MoneyFormatTest</b>
 * <p>Responsibility: Proves that amounts are shown with two decimals according to the language, and that input
 * is parsed the same with comma and period. Invalid input gives a validation error.</p>
 */
class MoneyFormatTest {

    private static final Locale SV = new Locale("sv", "SE");
    private static final Locale EN = Locale.ENGLISH;

    @Test
    void formatUsesCommaInSwedish() {
        assertEquals("12,50", MoneyFormat.format(Money.of("12.5"), SV));
        assertEquals("0,30", MoneyFormat.format(Money.of("0.1").add(Money.of("0.2")), SV));
    }

    @Test
    void formatUsesPointInEnglish() {
        assertEquals("12.50", MoneyFormat.format(Money.of("12.5"), EN));
        assertEquals("899.00", MoneyFormat.format(Money.of(899), EN));
    }

    @Test
    void formatIsEmpty_WhenMoneyIsNull() {
        assertEquals("", MoneyFormat.format(null, SV));
    }

    @Test
    void plainStyleHasNoThousandsSeparator() {
        assertEquals("1097,50", MoneyFormat.format(Money.of("1097.50"), SV, MoneyFormat.Style.PLAIN));
        assertEquals("1097.50", MoneyFormat.format(Money.of("1097.50"), EN, MoneyFormat.Style.PLAIN));
    }

    @Test
    void groupedStyleAddsThousandsSeparator() {
        char swedishSeparator = DecimalFormatSymbols.getInstance(SV).getGroupingSeparator();

        assertEquals("1" + swedishSeparator + "097,50",
                MoneyFormat.format(Money.of("1097.50"), SV, MoneyFormat.Style.GROUPED));
        assertEquals("1,097.50", MoneyFormat.format(Money.of("1097.50"), EN, MoneyFormat.Style.GROUPED));
        assertEquals("899,00", MoneyFormat.format(Money.of(899), SV, MoneyFormat.Style.GROUPED));
    }

    @ParameterizedTest(name = "\"{0}\" is parsed as {1}")
    @CsvSource({
            "'12,5', 12.50",
            "'12.5', 12.50",
            "'12', 12.00",
            "' 12,50 ', 12.50",
            "'1097,5', 1097.50",
            "'-5,25', -5.25",
            "',5', 0.50",
            "'.5', 0.50",
            "'-,5', -0.50"
    })
    void parseAcceptsCommaAndPoint(String text, String expected) {
        assertMoney(Money.of(expected), MoneyFormat.parse(text));
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"", "   ", "abc", "12,5,3", "12.5.3", "1 000", "12 kr", ",",
            "1,234", "12,345", "0.124", "1E3", "12,", ",123"})
    void parseThrowsValidationException_WhenTextIsNotAnAmount(String text) {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> MoneyFormat.parse(text));

        assertEquals("error.invalidAmount", exception.getMessageKey());
    }

    @Test
    void parseThrowsValidationException_WhenTextIsNull() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> MoneyFormat.parse(null));

        assertEquals("error.invalidAmount", exception.getMessageKey());
    }

    @ParameterizedTest(name = "{0} formatted and parsed again in {1}")
    @CsvSource({
            "0.00, sv",
            "0.30, sv",
            "1097.50, sv",
            "99999.99, sv",
            "-12.34, sv",
            "0.30, en",
            "1097.50, en",
            "99999.99, en",
            "-12.34, en"
    })
    void formattedTextCanBeParsedBack(String amount, String language) {
        Money original = Money.of(amount);
        Locale locale = new Locale(language);

        Money parsed = MoneyFormat.parse(MoneyFormat.format(original, locale));

        assertMoney(original, parsed);
    }
}
