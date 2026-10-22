package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


/**
 * Testklass som använder sig av ArchUnit för att utföra arkitekturs-tester för autocore-standalone.
 */
public class StandaloneArchitectureTest {

    /**
     * Testet använder {@link ClassFileImporter#importPackages(String...)} för att importera klasser
     * i och under {@code com.wac.autocore}, och validerar sedan att ingen importerad klass
     * är beroende av {@code javaFX}, eller klasser i {@code com.wac.autocore.view}.
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
     * Testet använder {@link ClassFileImporter#importPackages(String...)} för att importera klasser
     * i och under {@code com.wac.autocore.standalone}, och validerar sedan att ingen importerad klass
     * är beroende av klasser i {@code org.springframework..}. {@link ArchRule#allowEmptyShould(boolean)}
     * tillåter att testet går igenom även när inga klasser har importerats, till exempel om paketet
     * fortfarande är tomt.
     */
    @Test
    void standaloneMustNotDependOnSpringOrContext() {

        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.wac.autocore.standalone");

        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..").allowEmptyShould(true); // Tillåter att standalone inte har produktions-klasser.

        rule.check(classes);
    }
}
