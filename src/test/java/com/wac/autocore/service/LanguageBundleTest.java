package com.wac.autocore.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>LanguageBundleTest</b>
 * <p>Ansvar: Kontrollerar språkfilerna i CI: samma nycklar på båda språken, engelsk fallback,
 * korrekt teckenkodning, giltiga MessageFormat-mönster och att alla nycklar som används i koden finns.</p>
 */
class LanguageBundleTest {

    private static final String SV = "i18n/messages_sv.properties";
    private static final String EN = "i18n/messages_en.properties";
    private static final String BASE = "i18n/messages.properties";
    private static final String[] ALL_FILES = {SV, EN, BASE};

    private static final Path SOURCE_ROOT = Paths.get("src", "main", "java");

    // lang.get("nyckel" / lang.bind("nyckel" - fångar bara bokstavliga nycklar
    private static final Pattern KEY_USAGE = Pattern.compile("lang\\.(?:get|bind)\\(\\s*\"([^\"]+)\"");

    // Nycklar som byggs dynamiskt i koden (prefix + värde från databasen) och inte hittas av KEY_USAGE
    private static final String[] DYNAMIC_KEYS = {
            "booking.status.BOOKED", "booking.status.COMPLETED", "booking.status.CANCELLED",
            "booking.status.WORK_ORDER_CREATED", "booking.status.IN_PROGRESS",
            "workOrder.status.CREATED", "workOrder.status.IN_PROGRESS", "workOrder.status.COMPLETED",
            "payment.type.CARD", "payment.type.SWISH", "payment.type.CASH"
    };

    // Nycklar som används utanför lang.get/bind, t.ex. via NavigationItem.getKey()
    private static final String[] INDIRECT_KEYS = {
            "nav.home", "nav.customers", "nav.vehicles", "nav.bookings", "nav.serviceItems",
            "nav.mechanics", "nav.workOrders", "nav.invoices", "nav.payments"
    };

    private static Properties load(String path) throws IOException {
        try (InputStream in = LanguageBundleTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Missing resource: " + path);
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @Test
    void swedishAndEnglishHaveSameKeys() throws IOException {
        Set<String> sv = new TreeSet<>(load(SV).stringPropertyNames());
        Set<String> en = new TreeSet<>(load(EN).stringPropertyNames());

        Set<String> onlySv = new TreeSet<>(sv);
        onlySv.removeAll(en);
        Set<String> onlyEn = new TreeSet<>(en);
        onlyEn.removeAll(sv);

        assertTrue(onlySv.isEmpty(), "Keys missing in messages_en: " + onlySv);
        assertTrue(onlyEn.isEmpty(), "Keys missing in messages_sv: " + onlyEn);
    }

    @Test
    void fallbackFileIsEnglish() throws IOException {
        // messages.properties ska vara en exakt kopia av den engelska filen
        assertEquals(load(EN), load(BASE), "messages.properties differs from messages_en.properties");
    }

    @Test
    void noEmptyValues() throws IOException {
        for (String file : ALL_FILES) {
            Properties properties = load(file);
            for (String key : properties.stringPropertyNames()) {
                assertTrue(!properties.getProperty(key).trim().isEmpty(), file + " has an empty value for: " + key);
            }
        }
    }

    @Test
    void noBrokenEncoding() throws IOException {
        for (String file : ALL_FILES) {
            Properties properties = load(file);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                // U+FFFD = tecken som inte kunde läsas, "Ã" = UTF-8 läst som ISO-8859-1
                assertTrue(!value.contains("\uFFFD") && !value.contains("\u00C3"),
                        file + " has a broken character in key: " + key + " -> " + value);
            }
        }
    }

    @Test
    void placeholderPatternsAreValid() throws IOException {
        for (String file : ALL_FILES) {
            Properties properties = load(file);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                if (!value.contains("{")) {
                    continue;
                }
                try {
                    new MessageFormat(value);
                } catch (IllegalArgumentException e) {
                    fail(file + " has an invalid pattern in key: " + key + " -> " + e.getMessage());
                }
                // I texter med {0} måste apostrof skrivas '' annars försvinner den och resten tolkas fel
                assertTrue(!value.matches(".*(?<!')'(?!').*"),
                        file + " has a single apostrophe in a pattern, use '' instead: " + key);
            }
        }
    }

    @Test
    void dynamicAndIndirectKeysExist() throws IOException {
        Properties sv = load(SV);
        List<String> missing = new ArrayList<>();
        for (String key : concat(DYNAMIC_KEYS, INDIRECT_KEYS)) {
            if (!sv.containsKey(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "Keys missing in the language files: " + missing);
    }

    @Test
    void allKeysUsedInSourceExist() throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT), "Source folder not found: " + SOURCE_ROOT.toAbsolutePath());

        Properties sv = load(SV);
        Set<String> missing = new TreeSet<>();

        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            javaFiles = paths.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }

        for (Path file : javaFiles) {
            String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher matcher = KEY_USAGE.matcher(source);
            while (matcher.find()) {
                String key = matcher.group(1);
                // Nycklar som slutar på punkt är prefix för dynamiska nycklar (täcks av DYNAMIC_KEYS)
                if (!key.endsWith(".") && !sv.containsKey(key)) {
                    missing.add(key + "  (" + file.getFileName() + ")");
                }
            }
        }

        assertTrue(missing.isEmpty(), "Keys used in code but missing in the language files: " + missing);
    }

    private static String[] concat(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
