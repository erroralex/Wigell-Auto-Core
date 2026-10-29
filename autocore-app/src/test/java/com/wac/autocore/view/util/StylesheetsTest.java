package com.wac.autocore.view.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StylesheetsTest {

    // Fails fast if a file registered in Stylesheets is renamed or missing on the classpath
    @Test
    void allThemeStylesheetsExistOnClasspath() {
        assertFalse(assertDoesNotThrow(Stylesheets::themeUrls).isEmpty());
    }
}
