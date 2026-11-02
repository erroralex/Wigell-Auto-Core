package com.wac.autocore.data;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

/**
 * <b>LocalDateConverter</b>
 * <p>Responsibility: Stores LocalDate as TEXT in the correct format, matching V1__initial_schema.sql.</p>
 */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate,String> {

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String text) {
        return text == null ? null : LocalDate.parse(text);
    }
}
