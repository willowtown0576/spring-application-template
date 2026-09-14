import java.util.UUID

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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val integrationTest = sourceSets.create("integrationTest")
val architectureTest = sourceSets.create("architectureTest")
val codegen = sourceSets.create("codegen")

listOf(integrationTest, architectureTest).forEach {
    configurations[it.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
    configurations[it.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
    it.compileClasspath += sourceSets.main.get().output
    it.runtimeClasspath += sourceSets.main.get().output
}

dependencies {
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
    developmentOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    add(architectureTest.implementationConfigurationName, platform(libs.spring.modulith.bom))
    add(architectureTest.implementationConfigurationName, libs.spring.modulith.starter.test)
    add(integrationTest.implementationConfigurationName, libs.playwright)
    add(integrationTest.implementationConfigurationName, libs.spring.boot.testcontainers)
    add(integrationTest.implementationConfigurationName, libs.testcontainers.postgresql)
    add(integrationTest.implementationConfigurationName, libs.testcontainers.junit)
    add(codegen.implementationConfigurationName, platform(libs.spring.boot.bom))
    add(codegen.implementationConfigurationName, libs.jooq.codegen)
    add(codegen.implementationConfigurationName, libs.flyway.core)
    add(codegen.implementationConfigurationName, libs.flyway.postgresql)
    add(codegen.implementationConfigurationName, libs.testcontainers.postgresql)
    add(codegen.runtimeOnlyConfigurationName, libs.postgresql)
}

val jooqCodegen = tasks.register<JavaExec>("jooqCodegen") {
    description = "Generates jOOQ sources from a migrated temporary PostgreSQL database."
    group = "code generation"
    classpath = codegen.runtimeClasspath
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

tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
    auxClassPaths.from(sourceSets.main.get().output)
    excludeFilter = file("config/spotbugs/exclude.xml")
    classes = sourceSets.main.get().output.classesDirs.asFileTree.matching {
        exclude("dev/template/application/jooq/**")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val playwrightInstall = tasks.register<JavaExec>("playwrightInstall") {
    description = "Installs the Chromium browser used by UI integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    classpath = configurations[integrationTest.runtimeClasspathConfigurationName]
    mainClass = "com.microsoft.playwright.CLI"
    args("install", "chromium")
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    dependsOn(playwrightInstall, "vaadinBuildFrontend")
    environment("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
    useJUnitPlatform { excludeTags("documentation") }
    description = "Runs integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    systemProperty("test.postgres.image", libs.versions.postgres.image.get())
}

val architectureTestTask = tasks.register<Test>("architectureTest") {
    description = "Verifies application module boundaries."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = architectureTest.output.classesDirs
    classpath = architectureTest.runtimeClasspath
    shouldRunAfter(integrationTestTask)
    extensions.configure<JacocoTaskExtension> {
        isEnabled = false
    }
}

spotless {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java",
            "src/integrationTest/java/**/*.java", "src/architectureTest/java/**/*.java", "src/codegen/java/**/*.java")
        googleJavaFormat(libs.versions.google.java.format.get())
    }
    format("misc") {
        target("*.md", "docs/**/*.md", "*.gradle.kts", "gradle.properties", "gradle/*.toml",
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
    dependsOn(tasks.test, integrationTestTask)
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs.asFileTree.matching {
        exclude("dev/template/application/jooq/**")
    })
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")) {
        include("test.exec", "integrationTest.exec")
    })
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.check {
    dependsOn(integrationTestTask, architectureTestTask, tasks.jacocoTestReport)
}

// Documentation uses isolated Compose projects and never starts the development DB.
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
    classpath = codegen.runtimeClasspath
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

val projectDocumentation = tasks.register("projectDocumentation") {
    description = "Renders Mermaid diagrams and generates the Japanese project PDF."
    group = "documentation"
    dependsOn(documentationImage)
    inputs.file("docker/compose.yaml")
    inputs.dir("docs/project")
    inputs.dir("docker/docs")
    outputs.dir(layout.buildDirectory.dir("documentation/project"))
    doLast {
        val projectName = "starter-docs-pdf-" + UUID.randomUUID().toString().take(8)
        layout.buildDirectory.dir("documentation/project/diagrams").get().asFile.mkdirs()
        try {
            fileTree("docs/project/diagrams").matching { include("*.mmd") }.files.sorted().forEach { diagram ->
                docsCompose(projectName, "run", "--rm", "--no-deps", "docs-tools", "mmdc",
                    "-p", "/opt/docs/puppeteer.json", "-c", "/opt/docs/mermaid.json", "-i", "docs/project/diagrams/${diagram.name}",
                    "-o", "build/documentation/project/diagrams/${diagram.nameWithoutExtension}.svg", "-b", "transparent")
            }
            docsCompose(projectName, "run", "--rm", "--no-deps", "docs-tools", "pandoc",
                "docs/project/index.md", "--resource-path=build/documentation/project", "--pdf-engine=lualatex",
                "-o", "build/documentation/project/project.pdf")
        } finally {
            docsCompose(projectName, "down", "--volumes", "--remove-orphans")
        }
    }
}

val apiDocumentation = tasks.register<Test>("apiDocumentation") {
    description = "Exports and verifies springdoc OpenAPI YAML against an isolated test database."
    group = "documentation"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform { includeTags("documentation") }
    systemProperty("test.postgres.image", libs.versions.postgres.image.get())
    systemProperty("documentation.api.output", layout.buildDirectory.file("documentation/api/v1/openapi.yaml").get().asFile.absolutePath)
    outputs.file(layout.buildDirectory.file("documentation/api/v1/openapi.yaml"))
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

tasks.register("documentation") {
    description = "Generates database, API, and project documentation."
    group = "documentation"
    dependsOn(databaseDocumentation, apiDocumentation, projectDocumentation)
}

// Linux CI prerequisites remain behind the same Gradle command surface.
tasks.register<JavaExec>("playwrightInstallDeps") {
    description = "Installs Chromium system libraries on Linux (requires sudo)."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    classpath = configurations[integrationTest.runtimeClasspathConfigurationName]
    mainClass = "com.microsoft.playwright.CLI"
    args("install-deps", "chromium")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
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
    description = "Verifies and collects the JAR and documentation for a GitHub Release."
    group = "release"
    dependsOn(tasks.check, "documentation")
    from(tasks.named("bootJar"))
    from(databaseDocumentationZip)
    from(layout.buildDirectory.file("documentation/api/v1/openapi.yaml"))
    from(layout.buildDirectory.file("documentation/project/project.pdf"))
    into(layout.buildDirectory.dir("release"))
}
