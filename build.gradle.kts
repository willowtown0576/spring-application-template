import com.github.spotbugs.snom.SpotBugsTask
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun
import java.util.Properties
import java.util.UUID
import java.util.zip.ZipFile

plugins {
    java
    checkstyle
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.vaadin)
    alias(libs.plugins.spotless)
    alias(libs.plugins.spotbugs)
}

group = "dev.template"
version = "0.1.0-SNAPSHOT"

tasks.named<BootRun>("bootRun") {
    val localEnvironment = layout.projectDirectory.file("config/local-env.properties")
    doFirst {
        if (localEnvironment.asFile.exists()) {
            val values = Properties()
            localEnvironment.asFile.reader(Charsets.UTF_8).use { values.load(it) }
            values.stringPropertyNames().forEach { name ->
                if (!environment.containsKey(name)) {
                    environment(name, values.getProperty(name))
                }
            }
        }
        if (!environment.containsKey("STARTER_SECURITY_LOCAL_ENABLED")) {
            environment("STARTER_SECURITY_LOCAL_ENABLED", "true")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val codegenRuntime = configurations.create("codegenRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(codegenRuntime.name, project(":codegen"))
    implementation(platform(libs.vaadin.bom))
    implementation(libs.springdoc.webmvc.ui)
    implementation(libs.vaadin.core)
    implementation(libs.vaadin.spring)
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.modulith.bom))
    implementation(libs.spring.modulith.api)
    implementation(libs.uuid.creator)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.aspectj)
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.spring.boot.starter.batch.jdbc)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
    developmentOnly(platform(libs.spring.boot.bom))
    developmentOnly(platform(libs.vaadin.bom))
    developmentOnly(libs.vaadin.dev)
    developmentOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(platform(libs.spring.modulith.bom))
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.playwright)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

val jooqCodegen = tasks.register<JavaExec>("jooqCodegen") {
    description = "Generates jOOQ sources from a migrated temporary PostgreSQL database."
    group = "code generation"
    classpath = codegenRuntime
    mainClass = "dev.template.build.JooqCodegen"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    val migrations = layout.projectDirectory.dir("src/main/resources/db/migration")
    val output = layout.buildDirectory.dir("generated-src/jooq")
    inputs.dir(migrations).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("postgresImage", libs.versions.postgres.image)
    outputs.dir(output)
    args(libs.versions.postgres.image.get(), migrations.asFile.absolutePath, output.get().asFile.absolutePath)
}

sourceSets.main {
    java.srcDir(jooqCodegen)
}

tasks.named<Checkstyle>("checkstyleMain") {
    setSource(fileTree("src/main/java"))
}

tasks.named<SpotBugsTask>("spotbugsMain") {
    auxClassPaths.from(sourceSets.main.get().output)
    excludeFilter = file("config/spotbugs/exclude.xml")
    classes = sourceSets.main.get().output.classesDirs.asFileTree.matching {
        exclude("**/jooq/**")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation", "-Xlint:dep-ann", "-Xlint:removal", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val unitTestTask = tasks.register<Test>("unitTest") {
    description = "Runs domain and application unit tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { excludeTags("integration", "architecture", "documentation") }
}

val playwrightInstall = tasks.register<JavaExec>("playwrightInstall") {
    description = "Installs the Chromium browser used by UI integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args("install", "chromium")
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    dependsOn(playwrightInstall, "vaadinBuildFrontend")
    outputs.dir(layout.buildDirectory.dir("reports/ui"))
    environment("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
    useJUnitPlatform { includeTags("integration") }
    description = "Runs integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    shouldRunAfter(unitTestTask)
    systemProperty("test.postgres.image", libs.versions.postgres.image.get())
}

val architectureTestTask = tasks.register<Test>("architectureTest") {
    useJUnitPlatform { includeTags("architecture") }
    description = "Verifies application module boundaries."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    shouldRunAfter(integrationTestTask)
    extensions.configure<JacocoTaskExtension> {
        isEnabled = false
    }
}

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java", "codegen/src/main/java/**/*.java", "codegen/src/test/java/**/*.java")
        importOrder("#", "")
        removeUnusedImports()
        eclipse(libs.versions.eclipse.jdt.get()).configFile("config/formatter/eclipse-java.xml")
            .sortMembersEnabled(true)
            .sortMembersOrder("SF,SI,F,I,C,SM,M,T")
            .sortMembersDoNotSortFields(false)
            .sortMembersVisibilityOrderEnabled(true)
            .sortMembersVisibilityOrder("B,R,D,V")
    }
    format("misc") {
        target("*.md", "docs/**/*.md", "*.gradle.kts", "codegen/*.gradle.kts", "gradle.properties", "gradle/*.toml",
            ".gitignore", ".gitattributes", "config/**/*.xml", "src/main/resources/**/*.yml", "src/main/resources/**/*.sql", "docker/*.yaml", ".github/workflows/*.yml", "renovate.json")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    maxWarnings = 0
}

spotbugs {
    toolVersion = libs.versions.spotbugs.engine.get()
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    setDependsOn(listOf(unitTestTask, integrationTestTask))
    setMustRunAfter(listOf(unitTestTask, integrationTestTask))
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs.asFileTree.matching {
        exclude("**/jooq/**")
    })
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")) {
        include("unitTest.exec", "integrationTest.exec")
    })
    reports {
        xml.required = true
        html.required = true
    }
}

val allTestsReport = tasks.register<TestReport>("testReport") {
    description = "Combines unit, integration, and architecture test results into one HTML report."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    destinationDirectory = layout.buildDirectory.dir("reports/tests/all")
    testResults.from(unitTestTask, integrationTestTask, architectureTestTask)
}

tasks.test {
    description = "Runs unit, integration, and architecture tests and generates coverage."
    // Java plugin owns this Test task; dedicated tasks execute each suite once.
    reports.html.required = false
    reports.junitXml.required = false
    testClassesDirs = files()
    classpath = files()
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
    dependsOn(unitTestTask, integrationTestTask, architectureTestTask, allTestsReport, tasks.jacocoTestReport, ":codegen:test")
}

tasks.check {
    description = "Checks formatting, static analysis, and production JAR boundaries."
    setDependsOn(listOf("spotlessCheck", "checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest", ":codegen:check"))
}

tasks.build {
    description = "Runs checks and all tests, generates coverage, and assembles artifacts."
    dependsOn(tasks.test)
}

// Documentation uses a dedicated temporary database in an isolated Compose project.
val docsPassword = UUID.randomUUID().toString()
val docsDatabaseProject = "starter-docs-db-" + UUID.randomUUID().toString().take(8)
fun docsCompose(projectName: String, vararg arguments: String): String {
    val execution = providers.exec {
        isIgnoreExitValue = true
        environment("DEV_DB_PASSWORD", "unused-documentation")
        environment("DOCS_DB_PASSWORD", docsPassword)
        commandLine(listOf("docker", "compose", "-f", "docker/compose.yaml", "-p", projectName,
            "--profile", "docs") + arguments)
    }
    check(execution.result.get().exitValue == 0) {
        "Documentation Compose failed: " + (execution.standardError.asText.get() + execution.standardOutput.asText.get()).replace(docsPassword, "[REDACTED]")
    }
    return execution.standardOutput.asText.get().trim()
}

val documentationImage = tasks.register<Exec>("documentationImage") {
    description = "Builds the pinned documentation toolchain image."
    group = "documentation"
    commandLine("docker", "build", "-t", "spring-application-starter-docs:1", "docker/docs")
}

var docsDatabaseStarted = false
val stopDocumentationDatabase = tasks.register("stopDocumentationDatabase") {
    onlyIf { docsDatabaseStarted }
    doLast { docsCompose(docsDatabaseProject, "down", "--volumes", "--remove-orphans") }
}

val databaseDocumentation = tasks.register<JavaExec>("databaseDocumentation") {
    description = "Generates Markdown and SVG from a migrated temporary PostgreSQL schema."
    group = "documentation"
    dependsOn(documentationImage)
    finalizedBy(stopDocumentationDatabase)
    classpath = codegenRuntime
    mainClass = "dev.template.build.DocumentationMigration"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    inputs.dir("src/main/resources/db/migration")
    inputs.file(".tbls.yml")
    inputs.dir("docker/docs")
    inputs.file("docker/compose.yaml")
    outputs.dir(layout.buildDirectory.dir("documentation/database"))
    environment("DOCS_DB_PASSWORD", docsPassword)
    doFirst {
        layout.buildDirectory.dir("documentation").get().asFile.mkdirs()
        docsDatabaseStarted = true
        docsCompose(docsDatabaseProject, "up", "-d", "--wait", "docs-postgres")
        val port = docsCompose(docsDatabaseProject, "port", "docs-postgres", "5432").substringAfterLast(":")
        args("jdbc:postgresql://127.0.0.1:$port/documentation",
            layout.projectDirectory.dir("src/main/resources/db/migration").asFile.absolutePath)
    }
    doLast {
        docsCompose(docsDatabaseProject, "run", "--rm", "--no-deps", "docs-tools", "sh", "docker/docs/database.sh")
    }
}

val apiDocumentation = tasks.register<Test>("apiDocumentation") {
    description = "Exports and verifies springdoc OpenAPI YAML against an isolated test database."
    group = "documentation"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("documentation") }
    systemProperty("test.postgres.image", libs.versions.postgres.image.get())
    systemProperty("documentation.api.output", layout.buildDirectory.file("documentation/api/v1/openapi.yaml").get().asFile.absolutePath)
    outputs.file(layout.buildDirectory.file("documentation/api/v1/openapi.yaml"))
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

tasks.register("documentation") {
    description = "Generates database and API documentation."
    group = "documentation"
    dependsOn(databaseDocumentation, apiDocumentation)
}

// Linux CI prerequisites remain behind the same Gradle command surface.
tasks.register<JavaExec>("playwrightInstallDeps") {
    description = "Installs Chromium system libraries on Linux (requires sudo)."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args("install-deps", "chromium")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    builder.set(libs.versions.buildpack.builder.image)
    runImage.set(libs.versions.buildpack.run.image)
}

tasks.register("verifyReleaseVersion") {
    description = "Rejects non-stable releases and tags that differ from the project version."
    group = "release"
    val releaseTag = providers.gradleProperty("releaseTag")
    val applicationVersion = project.version.toString()
    doLast {
        val stableVersion = Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")
        check(stableVersion.matches(applicationVersion)) { "Release requires a stable SemVer project version." }
        check(releaseTag.orNull == "v$applicationVersion") { "releaseTag must equal v$applicationVersion." }
    }
}

val databaseDocumentationZip = tasks.register<Zip>("databaseDocumentationZip") {
    dependsOn(databaseDocumentation)
    from(layout.buildDirectory.dir("documentation/database"))
    archiveFileName.set("database-documentation.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.register<Sync>("releaseArtifacts") {
    description = "Generates and collects the JAR and documentation for a GitHub Release."
    group = "release"
    dependsOn("documentation")
    from(tasks.named("bootJar"))
    from(databaseDocumentationZip)
    from(layout.buildDirectory.file("documentation/api/v1/openapi.yaml"))
    into(layout.buildDirectory.dir("release"))
}

val verifyProductionJar = tasks.register("verifyProductionJar") {
    description = "Verifies that build tools and test classes stay out of the production JAR."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    val bootJar = tasks.named<BootJar>("bootJar")
    val excludedOutputs = files(sourceSets.test.get().output.classesDirs,
        project(":codegen").layout.buildDirectory.dir("classes/java/main"))
    dependsOn(bootJar, tasks.testClasses, ":codegen:classes")
    inputs.file(bootJar.flatMap { it.archiveFile })
    inputs.files(excludedOutputs)
    val mainOutputs = sourceSets.main.get().output.classesDirs
    inputs.files(mainOutputs)
    doLast {
        ZipFile(bootJar.get().archiveFile.get().asFile).use { jar ->
            excludedOutputs.files.filter { it.isDirectory }.forEach { directory ->
                directory.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { compiled ->
                    val entry = "BOOT-INF/classes/" + compiled.relativeTo(directory).invariantSeparatorsPath
                    check(jar.getEntry(entry) == null) { "Non-production class packaged: $entry" }
                }
            }
            check(jar.entries().asSequence().none { it.name.startsWith("BOOT-INF/lib/codegen") }) {
                "Build tool JAR must not be packaged in the application."
            }
            mainOutputs.files.filter { it.isDirectory }.forEach { directory ->
                directory.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { compiled ->
                    val entry = "BOOT-INF/classes/" + compiled.relativeTo(directory).invariantSeparatorsPath
                    check(jar.getEntry(entry) != null) { "Production class missing from JAR: $entry" }
                }
            }
        }
    }
}

tasks.check { dependsOn(verifyProductionJar) }
