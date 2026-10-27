package com.wac.autocore.standalone;

import com.wac.autocore.i18n.ResourceBundleTranslator;
import com.wac.autocore.i18n.Translator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandaloneMessagesTest {

    @Test
    void standaloneFindsTheSameTextsAsTheGui() {
        Translator sv = new ResourceBundleTranslator(ResourceBundleTranslator.SWEDISH);
        Translator en = new ResourceBundleTranslator(ResourceBundleTranslator.ENGLISH);

        assertEquals("Åtkomst nekad", sv.get("error.accessDenied"));
        assertEquals("Access denied", en.get("error.accessDenied"));
    }
}
