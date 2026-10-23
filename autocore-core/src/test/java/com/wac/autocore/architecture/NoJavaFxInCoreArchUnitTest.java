package com.wac.autocore.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * An architecture test using {@code ArchUnit} that does more or less the same thing as {@code NoJavaFxInCoreTest.java}.
 * Mostly created as a comparison between the two different tests for educational purposes.
 *
 * The ArchUnit test looks at compiled bytecode, including annotations and signatures, but does not catch references
 * that are only used in strings. Nor does it check that the expected directories exist (contain classes).
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
