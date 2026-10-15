package com.wac.autocore.unit;

import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>MoneyConverterTest</b>
 * <p>Ansvar: Bevisar att Money sparas som REAL och läses tillbaka oförändrat, och att
 * gamla double-värden i databasen avrundas till två decimaler vid läsning.</p>
 */
class MoneyConverterTest {

    private final MoneyConverter converter = new MoneyConverter();

    @ParameterizedTest(name = "{0} survives a round trip")
    @ValueSource(strings = {"0.00", "0.01", "0.10", "0.30", "449.50", "899.00", "1097.50", "99999.99", "-12.34"})
    void roundTripKeepsTheAmount(String text) {
        Money original = Money.of(text);

        Double stored = converter.convertToDatabaseColumn(original);
        Money restored = converter.convertToEntityAttribute(stored);

        assertMoney(original, restored);
    }

    @Test
    void convertsToTheSameNumberThatTheOldColumnsHeld() {
        assertEquals(Double.valueOf(1097.5), converter.convertToDatabaseColumn(Money.of("1097.50")));
        assertEquals(Double.valueOf(899.0), converter.convertToDatabaseColumn(Money.of(899)));
    }

    @Test
    @DisplayName("0.1 + 0.2 is stored and read back as 0.30")
    void sumOfPointOneAndPointTwoIsStoredAsPointThirty() {
        Money sum = Money.of("0.1").add(Money.of("0.2"));

        Money restored = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(sum));

        assertMoney(Money.of("0.30"), restored);
        assertEquals("0.30", restored.toString());
    }

    @ParameterizedTest(name = "stored {0} is read as {1}")
    @CsvSource({
            "0.30000000000000004, 0.30",
            "0.125, 0.13",
            "0.124, 0.12",
            "2.675, 2.68",
            "1097.499999, 1097.50",
            "449.5, 449.50",
            "899.0, 899.00",
            "-0.125, -0.13"
    })
    void readingRoundsToTwoDecimalsHalfUp(double stored, String expected) {
        Money read = converter.convertToEntityAttribute(stored);

        assertMoney(Money.of(expected), read);
        assertEquals(expected, read.toString());
    }

    @Test
    void nullIsWrittenAsNull_AndReadAsZero() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertMoney(Money.ZERO, converter.convertToEntityAttribute(null));
    }
}
