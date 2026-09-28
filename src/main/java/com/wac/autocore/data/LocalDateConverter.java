package com.wac.autocore.data;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.LocalDate;

/**
 * <b>LocalDateConverter</b>
 * <p>Ansvar: LocalDate som TEXT med korrekt format matchande schema.sql.</p>
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
