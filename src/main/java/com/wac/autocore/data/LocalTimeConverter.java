package com.wac.autocore.data;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * <b>LocalTimeConverter</b>
 * <p>Ansvar: LocalTime som TEXT med korrekt format matchande schema.sql.</p>
 */
@Converter(autoApply = true)
public class LocalTimeConverter implements AttributeConverter<LocalTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalTime time) {
        return time == null ? null : time.format(FORMAT);
    }

    @Override
    public LocalTime convertToEntityAttribute(String text) {
        return text == null ? null : LocalTime.parse(text, FORMAT);
    }
}
