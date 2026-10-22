package com.wac.autocore.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Ett arkitekturs-test genom {@code ArchUnit} som gör mer eller mindre samma sak som {@code NoJavaFxInCoreTest.java}.
 * Mest skapad som en jämförelse mellan de två olika testerna för utbildningssyfte.
 *
 * ArchUnit testet kollar på kompilerad byte-kod, inklusive annoteringar och signaturen, men fångar inte referenser
 * som bara används i strängar. Detta kollar inte heller så att förväntade directories existerar (innehåller klasser).
 */
public class NoJavaFxInCoreArchUnitTest {

    @Test
    void coreMustNotDependOnJavaFxOrView() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.wac.autocore.model",
                        "com.wac.autocore.service",
                        "com.wac.autocore.repository",
                        "com.wac.autocore.exception",
                        "com.wac.autocore.i18n",
                        "com.wac.autocore.headless");

        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("javafx..", "com.wac.autocore.view..");

        rule.check(classes);
    }
}
