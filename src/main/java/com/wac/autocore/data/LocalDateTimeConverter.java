package com.wac.autocore.data;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <b>LocalDateTimeConverter</b>
 * <p>Ansvar: LocalDateTime som TEXT med korrekt format matchande schema.sql.</p>
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(FORMAT);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String text) {
        return text == null ? null : LocalDateTime.parse(text, FORMAT);
    }
}
