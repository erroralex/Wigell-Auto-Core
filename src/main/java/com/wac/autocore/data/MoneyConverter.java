package com.wac.autocore.data;

import com.wac.autocore.model.value.Money;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;

// Money som REAL i databasen, så att kolumnerna i V1-V10 inte ändras.
// Vid läsning avrundas värdet till två decimaler med HALF_UP, så att det alltid är samma som i Money-objektet.
// Det är bara här som double förekommer, eftersom kolumnen är REAL.
// Null: ett saknat värde i databasen (t.ex. nullable invoice_line) läses som Money.ZERO, så Money är aldrig null i modellen.
// Skrivning av null ger fortsatt null, så att ett saknat belopp i en NOT NULL-kolumn ger ett tydligt fel och inte tyst 0.
@Converter
public class MoneyConverter implements AttributeConverter<Money, Double> {

    @Override
    public Double convertToDatabaseColumn(Money money) {
        return money != null ? money.toBigDecimal().doubleValue() : null;
    }

    @Override
    public Money convertToEntityAttribute(Double value) {
        return value == null ? Money.ZERO : Money.of(BigDecimal.valueOf(value));
    }

}
