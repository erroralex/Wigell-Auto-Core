package com.wac.autocore.architecture;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>NoJavaFxInCoreTest</b>
 * <p>Ansvar: Säkerställer att kärnpaketen inte är beroende av JavaFX, så att de kan köras
 * utan grafiskt gränssnitt (headless) och på en JRE utan JavaFX.</p>
 * <p>Testet läser källkoden i stället för de kompilerade klasserna. Det letar efter
 * {@code javafx.} på alla rader som inte är kommentarer, så att även fullt kvalificerade
 * anrop utan import fångas, t.ex. {@code javafx.scene.paint.Color.web(...)}.</p>
 * <p>{@code i18n} och {@code headless} ingår eftersom de används av både UI och headless.</p>
 */
class NoJavaFxInCoreTest {

    private static final Path ROOT = Paths.get("src", "main", "java", "com", "wac", "autocore");

    @ParameterizedTest(name = "{0} importerar inte JavaFX")
    @ValueSource(strings = {"model", "service", "repository", "exception", "i18n", "headless"})
    void packageDoesNotUseJavaFx(String packageName) throws IOException {
        Path packageDir = ROOT.resolve(packageName);

        // Annars skulle ett omdöpt eller borttaget paket tyst ge grönt test
        assertTrue(Files.isDirectory(packageDir),
                () -> "Paketet saknas: " + packageDir.toAbsolutePath());

        List<String> violations;
        try (Stream<Path> files = Files.walk(packageDir)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(NoJavaFxInCoreTest::findJavaFxUsages)
                    .collect(Collectors.toList());
        }

        assertTrue(violations.isEmpty(),
                () -> "JavaFX används i paketet '" + packageName + "':\n"
                        + String.join("\n", violations));
    }

    // Returnerar "Fil.java:rad: kod" för varje rad som använder JavaFX
    private static Stream<String> findJavaFxUsages(Path file) {
        List<String> lines = readLines(file);
        List<String> hits = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!isComment(line) && line.contains("javafx.")) {
                hits.add(ROOT.relativize(file) + ":" + (i + 1) + ": " + line);
            }
        }
        return hits.stream();
    }

    // Javadoc och kommentarer får nämna JavaFX, t.ex. i DomainException
    private static boolean isComment(String trimmedLine) {
        return trimmedLine.startsWith("//")
                || trimmedLine.startsWith("*")
                || trimmedLine.startsWith("/*");
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Kunde inte läsa " + file, e);
        }
    }
}
