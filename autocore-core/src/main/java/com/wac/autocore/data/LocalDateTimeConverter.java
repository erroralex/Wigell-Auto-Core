package com.wac.autocore.data;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <b>LocalDateTimeConverter</b>
 * <p>Responsibility: Stores LocalDateTime as TEXT in the correct format, matching V1__initial_schema.sql.</p>
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
