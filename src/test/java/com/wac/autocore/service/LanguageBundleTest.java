package com.wac.autocore.service;

import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.view.NavigationItem;
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

    // Payment type är en String, inte en enum, så nycklarna kan inte härledas från en enum.
    private static final String[] PAYMENT_TYPE_KEYS = {
            "payment.type.CARD", "payment.type.SWISH", "payment.type.CASH"
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
                assertTrue(!value.matches(".*(?<!')'(?!').*"),
                        file + " has a single apostrophe in a pattern, use '' instead: " + key);
            }
        }
    }

    @Test
    void dynamicAndIndirectKeysExist() throws IOException {
        Properties sv = load(SV);
        List<String> missing = new ArrayList<>();

        for (BookingStatus status : BookingStatus.values()) {
            String key = "booking.status." + status.name();
            if (!sv.containsKey(key)) {
                missing.add(key);
            }
        }

        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            String key = "workOrder.status." + status.name();
            if (!sv.containsKey(key)) {
                missing.add(key);
            }
        }

        for (NavigationItem item : NavigationItem.values()) {
            if (!sv.containsKey(item.getKey())) {
                missing.add(item.getKey());
            }
        }

        for (String key : PAYMENT_TYPE_KEYS) {
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
                if (!key.endsWith(".") && !sv.containsKey(key)) {
                    missing.add(key + "  (" + file.getFileName() + ")");
                }
            }
        }

        assertTrue(missing.isEmpty(), "Keys used in code but missing in the language files: " + missing);
    }
}