package dev.template.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.internal.domain.Feature;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.transaction.annotation.Transactional;

class ArchitectureTest {
  private static final ImportOption PRODUCTION =
      location ->
          ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.includes(location)
              && !location.contains("/architectureTest/")
              && !location.contains("/integrationTest/");
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(PRODUCTION)
          .importPackages("dev.template.application");

  @Test
  void verifiesModuleBoundariesAndPublicInterfaces() {
    var modules = ApplicationModules.of(Application.class, PRODUCTION);
    modules.verify();
    var feature = modules.getModuleByName("feature").orElseThrow();
    assertThat(feature.getNamedInterfaces().getByName("command")).isPresent();
    assertThat(feature.getNamedInterfaces().getByName("query")).isPresent();
    assertThat(feature.isExposed(FeatureCommands.class)).isTrue();
    assertThat(feature.isExposed(FeatureQueries.class)).isTrue();
    assertThat(feature.isExposed(Feature.class)).isFalse();
  }

  @Test
  void domainAndUseCasesAreIndependentOfFrameworks() {
    noClasses()
        .that()
        .resideInAnyPackage("..internal.domain..", "..internal.usecase..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "org.jooq..",
            "com.vaadin..",
            "jakarta..",
            "com.github.f4b6a3..",
            "..application.jooq..")
        .check(CLASSES);
  }

  @Test
  void queryBypassesDomainAndUseCases() {
    noClasses()
        .that()
        .resideInAPackage("..internal.query..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..internal.domain..", "..internal.usecase..")
        .check(CLASSES);
  }

  @Test
  void generatedTypesAreUsedOnlyByInfrastructure() {
    noClasses()
        .that()
        .resideOutsideOfPackages("..internal.infrastructure..", "..application.jooq..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..application.jooq..")
        .check(CLASSES);
  }

  @Test
  void webDoesNotUseSqlDirectly() {
    noClasses()
        .that()
        .resideInAPackage("..web..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.jooq..")
        .check(CLASSES);
  }

  @Test
  void businessBoundariesDeclareAuthorization() {
    methods()
        .that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .beAnnotatedWith(org.springframework.security.access.prepost.PreAuthorize.class)
        .check(CLASSES);
  }

  @Test
  void transactionsBelongToCommandAndQueryBoundaries() {
    methods()
        .that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .beDeclaredInClassesThat()
        .resideInAnyPackage("..internal.command..", "..internal.query..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideOutsideOfPackages("..internal.command..", "..internal.query..")
        .should()
        .beAnnotatedWith(Transactional.class)
        .check(CLASSES);
  }
}
