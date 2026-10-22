package architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class StandaloneArchitectureTest {

    @Test
    void standaloneMustNotDependOnJavaFxOrView() {

        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.wac.autocore");

        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("javafx..", "com.wac.autocore.view");

        rule.check(classes);
    }

    @Test
    void standaloneMustNotDependOnSpring() {

        JavaClasses classes = new ClassFileImporter()
                .importPackages("com.wac.autocore.standalone");

        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..").allowEmptyShould(true); // Tillåter att standalone inte har produktions-klasser.

        rule.check(classes);
    }
}
