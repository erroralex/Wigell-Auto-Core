package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


/**
 * Test class that uses ArchUnit to run architecture tests for autocore-standalone.
 */
public class StandaloneArchitectureTest {

    /**
     * The test uses {@link ClassFileImporter#importPackages(String...)} to import classes
     * in and under {@code com.wac.autocore}, and then validates that no imported class
     * depends on {@code javaFX}, or classes in {@code com.wac.autocore.view}.
     */
    @Test
    void standaloneMustNotDependOnJavaFxOrView() {

        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.wac.autocore");

        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("javafx..", "com.wac.autocore.view");

        rule.check(classes);
    }

    /**
     * The test uses {@link ClassFileImporter#importPackages(String...)} to import classes
     * in and under {@code com.wac.autocore.standalone}, and then validates that no imported class
     * depends on classes in {@code org.springframework..}. {@link ArchRule#allowEmptyShould(boolean)}
     * allows the test to pass even when no classes have been imported, for example if the package
     * is still empty.
     */
    @Test
    void standaloneMustNotDependOnSpringOrContext() {

        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.wac.autocore.standalone");

        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..").allowEmptyShould(true); // Allows standalone to have no production classes.

        rule.check(classes);
    }
}
