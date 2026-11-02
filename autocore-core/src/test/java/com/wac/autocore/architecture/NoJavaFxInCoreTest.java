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
 * <p>Responsibility: Ensures that the core packages do not depend on JavaFX, so they can run
 * without a graphical interface (headless) and on a JRE without JavaFX.</p>
 * <p>The test reads the source code instead of the compiled classes. It looks for
 * {@code javafx.} on all lines that are not comments, so that even fully qualified
 * calls without an import are caught, e.g. {@code javafx.scene.paint.Color.web(...)}.</p>
 * <p>It also forbids references to {@code com.wac.autocore.view}. A plain text search does not see
 * transitive dependencies, and the view layer is the only path to JavaFX in the project. Closing
 * that path means the core cannot indirectly depend on JavaFX either.</p>
 * <p>{@code i18n} and {@code headless} are included because they are used by both the UI and headless.</p>
 */
class NoJavaFxInCoreTest {

    private static final Path ROOT = Paths.get( "src", "main", "java", "com", "wac", "autocore");

    // JavaFX directly, or the view layer which in turn uses JavaFX
    private static final String[] FORBIDDEN = {"javafx.", "com.wac.autocore.view"};

    @ParameterizedTest(name = "{0} is independent of JavaFX and view")
    @ValueSource(strings = {"model", "service", "repository", "exception", "i18n", "headless"})
    void packageDoesNotUseJavaFx(String packageName) throws IOException {
        Path packageDir = ROOT.resolve(packageName);

        // Otherwise a renamed or removed package would silently give a green test
        assertTrue(Files.isDirectory(packageDir),
                () -> "The package is missing: " + packageDir.toAbsolutePath());

        List<String> violations;
        try (Stream<Path> files = Files.walk(packageDir)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(NoJavaFxInCoreTest::findForbiddenReferences)
                    .collect(Collectors.toList());
        }

        assertTrue(violations.isEmpty(),
                () -> "JavaFX or view is used in the package '" + packageName + "':\n"
                        + String.join("\n", violations));
    }

    // Returns "File.java:line: code" for each line that refers to JavaFX or the view layer
    private static Stream<String> findForbiddenReferences(Path file) {
        List<String> lines = readLines(file);
        List<String> hits = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!isComment(line) && containsForbidden(line)) {
                hits.add(ROOT.relativize(file) + ":" + (i + 1) + ": " + line);
            }
        }
        return hits.stream();
    }

    private static boolean containsForbidden(String line) {
        for (String forbidden : FORBIDDEN) {
            if (line.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }

    // Javadoc and comments may mention JavaFX, e.g. in DomainException
    private static boolean isComment(String trimmedLine) {
        return trimmedLine.startsWith("//")
                || trimmedLine.startsWith("*")
                || trimmedLine.startsWith("/*");
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }
}
