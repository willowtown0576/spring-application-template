package dev.template.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import dev.template.application.security.SystemExecution;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/** Modulithを正本としてmodule境界を検証し、追加の技術依存規則だけArchUnitで補う。 */
@Tag("architecture")
class ArchitectureTest {
    /** 依存方向と認可・transaction境界を検査するproduction class群。 */
    private static final JavaClasses CLASSES;
    /** architecture検査からtest classを除外するimport条件。 */
    private static final ImportOption PRODUCTION = ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;

    static {
        CLASSES = new ClassFileImporter().withImportOption(PRODUCTION)
            .importPackages(Application.class.getPackageName());
    }

    /** 公開Command／Queryの実装にtransactionと認可があり、QueryがreadOnlyであることを検証する。 */
    @Test
    void businessBoundariesDeclareAuthorization() {
        methods().that().areAnnotatedWith(Transactional.class).should().beMetaAnnotatedWith(PreAuthorize.class)
            .check(CLASSES);

        // 公開APIの契約から検査対象の実装を取得する。
        for (final var implementation : CLASSES) {
            if (implementation.isInterface()) {
                continue;
            }
            for (final var api : implementation.getAllRawInterfaces()) {
                if (!api.getPackageName().matches(".*\\.api\\.(command|query)(\\..*)?")) {
                    continue;
                }
                // 検査対象は外部から呼び出す公開業務操作とする。
                for (final var contract : api.getMethods()) {
                    if (contract.getModifiers().contains(JavaModifier.STATIC)
                        || contract.getModifiers().contains(JavaModifier.PRIVATE)) {
                        continue;
                    }
                    final var method = implementation.getMethod(contract.getName(),
                        contract.getRawParameterTypes().stream().map(type -> type.getName()).toArray(String[]::new));
                    assertThat(method.isAnnotatedWith(Transactional.class)).as(method.getFullName()).isTrue();
                    assertThat(
                        method.isMetaAnnotatedWith(PreAuthorize.class) || method.isAnnotatedWith(PreAuthorize.class))
                        .as(method.getFullName()).isTrue();
                    assertThat(method.getAnnotationOfType(Transactional.class).readOnly()).as(method.getFullName())
                        .isEqualTo(api.getPackageName().matches(".*\\.api\\.query(\\..*)?"));
                }
            }
        }
    }

    /** DataSourceのinterfaceと実装を解析し、公開Queryの結果型へ依存していないことを検証する。 */
    @Test
    void dataSourcesDoNotDependOnPublicQueryResults() {
        noClasses().that().haveSimpleNameEndingWith("DataSource").should().dependOnClassesThat()
            .resideInAPackage("..api.query..").check(CLASSES);
    }

    /** domainとUseCaseを解析し、SQL・Web・Security等の実装型へ依存していないことを検証する。 */
    @Test
    void domainAndUseCasesAreIndependentOfFrameworks() {
        noClasses().that().resideInAnyPackage("..internal.domain..", "..internal.usecase..").should()
            .dependOnClassesThat().resideInAnyPackage("org.jooq..", "com.vaadin..", "jakarta..", "com.github.f4b6a3..",
                Application.class.getPackageName() + ".jooq..")
            .check(CLASSES);
    }

    /** domainはSpring非依存、UseCaseはService登録annotation以外のSpring型へ非依存であることを検証する。 */
    @Test
    void domainAndUseCasesRestrictSpringDependencies() {
        noClasses().that().resideInAPackage("..internal.domain..").should().dependOnClassesThat()
            .resideInAPackage("org.springframework..").check(CLASSES);
        noClasses().that().resideInAPackage("..internal.usecase..").should()
            .dependOnClassesThat(DescribedPredicate.describe("Spring types other than Service",
                type -> type.getPackageName().startsWith("org.springframework")
                    && !type.getName().equals("org.springframework.stereotype.Service")))
            .check(CLASSES);
    }

    /** generated jOOQ型への参照がinfrastructure内に限定されていることを検証する。 */
    @Test
    void generatedTypesAreUsedOnlyByInfrastructure() {
        noClasses().that()
            .resideOutsideOfPackages("..internal.infrastructure..", Application.class.getPackageName() + ".jooq..")
            .should().dependOnClassesThat().resideInAPackage(Application.class.getPackageName() + ".jooq..")
            .check(CLASSES);
    }

    /** system主体での実行境界を、信頼されたprocess内adapterに限定する。 */
    @Test
    void onlyBackgroundAdaptersCanEstablishSystemIdentity() {
        noClasses().that().resideOutsideOfPackages("..security..", "..batch..", "..scheduling..").should()
            .dependOnClassesThat().haveFullyQualifiedName(SystemExecution.class.getName()).check(CLASSES);
    }

    /** Query実装の依存先がDataSourceによる参照経路に適合することを検証する。 */
    @Test
    void queryBypassesDomainAndUseCases() {
        noClasses().that().resideInAPackage("..internal.query..").should().dependOnClassesThat()
            .resideInAnyPackage("..internal.domain..", "..internal.usecase..").check(CLASSES);
    }

    /** Transactionalの配置を解析し、Command/Query境界以外へtransaction責務が漏れないことを検証する。 */
    @Test
    void transactionsBelongToCommandAndQueryBoundaries() {
        methods().that().areAnnotatedWith(Transactional.class).should().beDeclaredInClassesThat()
            .resideInAnyPackage("..internal.command..", "..internal.query..").check(CLASSES);
        noClasses().that().resideOutsideOfPackages("..internal.command..", "..internal.query..").should()
            .beAnnotatedWith(Transactional.class).check(CLASSES);
    }

    /** production classをModulithで解析し、循環依存がなく公開APIだけがmodule外へ公開されることを検証する。 */
    @Test
    void verifiesModuleBoundariesAndPublicInterfaces() {
        ApplicationModules.of(Application.class, PRODUCTION).verify();

    }
    /** Web adapterを解析し、jOOQによる直接SQL操作へ依存していないことを検証する。 */
    @Test
    void webDoesNotUseSqlDirectly() {
        noClasses().that().resideInAPackage("..web..").should().dependOnClassesThat().resideInAPackage("org.jooq..")
            .check(CLASSES);
    }
}
