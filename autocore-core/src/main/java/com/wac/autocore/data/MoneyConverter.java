package com.wac.autocore.data;

import com.wac.autocore.model.value.Money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

// Money is stored as REAL in the database, so the columns in V1-V10 are not changed.
// When reading, the value is rounded to two decimals with HALF_UP, so it always equals the value in the Money object.
// This is the only place where double is used, because the column is REAL.
// Null: a missing value in the database (e.g. nullable invoice_line) is read as Money.ZERO, so Money is never null in the model.
// Writing null still yields null, so a missing amount in a NOT NULL column gives a clear error instead of a silent 0.
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
