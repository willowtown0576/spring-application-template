plugins {
    java
    checkstyle
    alias(libs.plugins.spotbugs)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
repositories { mavenCentral() }

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.jooq.codegen)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:dep-ann", "-Xlint:removal", "-Werror"))
}
checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}
spotbugs { toolVersion = libs.versions.spotbugs.engine.get() }

val unitTestTask = tasks.register<Test>("unitTest") {
    description = "Runs code generation tool unit tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}

tasks.test {
    description = "Runs all code generation tool tests."
    testClassesDirs = files()
    classpath = files()
    dependsOn(unitTestTask)
}

tasks.check {
    description = "Runs static analysis."
    setDependsOn(listOf("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest"))
}

tasks.build { dependsOn(tasks.test) }
