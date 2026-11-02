package com.wac.autocore.standalone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>StandaloneMainProcessTest</b>
 * <p>Responsibility: Proves that the standalone main method runs as a separate program, exits with code 0 and
 * starts no Spring context.</p>
 * <p>{@code StandaloneMain.main} ends with {@code System.exit}, so it cannot be called inside the test JVM.
 * The test starts a new JVM with the same classpath and checks its exit code and output.</p>
 */
class StandaloneMainProcessTest {

    private static final long TIMEOUT_SECONDS = 60;

    @ParameterizedTest(name = "--role={0} exits with 0")
    @ValueSource(strings = {"ADMIN", "WORKSHOP", "READ_ONLY"})
    @DisplayName("main exits with 0 and starts no Spring context for every role")
    void mainExitsWithZeroAndStartsNoSpring(String role) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                StandaloneMain.class.getName(),
                "--role=" + role)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(finished, "The program did not finish in time. Output:\n" + output);
        assertEquals(0, process.exitValue(), "Unexpected exit code. Output:\n" + output);
        assertFalse(output.contains("Spring"), "Spring output found:\n" + output);
    }
}
