package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.persistence.Convert;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>MoneyMappingGuardTest</b>
 * <p>Responsibility: Proves that every REAL column in the real schema belongs to a Money field with
 * {@link MoneyConverter}, and that no monetary field has become double or BigDecimal.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class MoneyMappingGuardTest {

    private static final Set<String> EXPECTED_REAL_COLUMNS = new TreeSet<>(Arrays.asList(
            "service_item.price",
            "invoice.amount",
            "invoice.discount",
            "invoice.total_amount",
            "payment.amount",
            "booking_service_item.price_at_booking",
            "work_order_service_item.agreed_price",
            "invoice_line.amount",
            "invoice_line.discount",
            "invoice_line.total"
    ));

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("money-mapping-guard.db").toAbsolutePath());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("The schema has exactly the ten known REAL columns")
    void schemaHasExactlyTheKnownRealColumns() {
        Set<String> realColumns = new TreeSet<>();

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
                String.class);

        for (String table : tables) {
            for (Map<String, Object> column : jdbcTemplate.queryForList("PRAGMA table_info(" + table + ")")) {
                if ("REAL".equalsIgnoreCase(String.valueOf(column.get("type")))) {
                    realColumns.add(table + "." + column.get("name"));
                }
            }
        }

        assertEquals(EXPECTED_REAL_COLUMNS, realColumns);
    }

    @Test
    @DisplayName("Every Money field has @Convert(MoneyConverter), and there are as many as REAL columns")
    void everyMoneyFieldIsConverted() {
        Set<String> moneyFields = new HashSet<>();

        for (ManagedType<?> type : entityManagerFactory.getMetamodel().getManagedTypes()) {
            for (Attribute<?, ?> attribute : type.getDeclaredAttributes()) {
                if (attribute.getJavaType() != Money.class) {
                    continue;
                }
                Field field = (Field) attribute.getJavaMember();
                Convert convert = field.getAnnotation(Convert.class);

                assertTrue(convert != null && convert.converter() == MoneyConverter.class,
                        type.getJavaType().getSimpleName() + "." + attribute.getName()
                                + " is Money but has no @Convert(MoneyConverter)");
                moneyFields.add(type.getJavaType().getSimpleName() + "." + attribute.getName());
            }
        }

        assertEquals(EXPECTED_REAL_COLUMNS.size(), moneyFields.size(), "Money fields: " + moneyFields);
    }

    @Test
    @DisplayName("No entity or embeddable has a double, float or BigDecimal attribute")
    void noFloatingPointOrBigDecimalAttributes() {
        Set<Class<?>> forbidden = new HashSet<>(Arrays.<Class<?>>asList(
                double.class, Double.class, float.class, Float.class, BigDecimal.class));

        for (ManagedType<?> type : entityManagerFactory.getMetamodel().getManagedTypes()) {
            for (Attribute<?, ?> attribute : type.getDeclaredAttributes()) {
                assertFalse(forbidden.contains(attribute.getJavaType()),
                        type.getJavaType().getSimpleName() + "." + attribute.getName()
                                + " must be Money, not " + attribute.getJavaType().getSimpleName());
            }
        }
    }
}
