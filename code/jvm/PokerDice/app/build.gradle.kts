plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":http"))
    implementation(project(":repo-jdbi"))
    implementation(project(":domain"))
    implementation(project(":service"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // for JDBI and Postgres
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.jdbi:jdbi3-kotlin:3.37.1")
    implementation("org.jdbi:jdbi3-postgres:3.37.1")
    implementation("org.postgresql:postgresql")

    // To use WebTestClient in integration tests with real HTTP server
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")

    // To automatically run the Spring MVC web server in coordination with unit tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootRun {
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=pokerdice&password=password")
}

tasks.test {
    useJUnitPlatform()
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=pokerdice&password=password")
}

kotlin {
    jvmToolchain(21)
}

/**
 * Docker related tasks
 */
val dockerImageJvm = "pokerdice-jvm"
val dockerImagePostgres = "pokerdice-postgres"
val dockerImageUbuntu = "pokerdice-ubuntu"
val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker" // Linux and others
    }

tasks.register<Copy>("extractUberJar") {
    dependsOn("assemble")
    // opens the JAR containing everything...
    from(
        zipTree(
            layout.buildDirectory
                .file("libs/app-$version.jar")
                .get()
                .toString(),
        ),
    )
    // ... into the 'build/dependency' folder
    into("build/dependency")
}

tasks.register<Exec>("buildImageJvm") {
    dependsOn("extractUberJar")
    commandLine(dockerExe, "build", "-t", dockerImageJvm, "-f", "../docker/Dockerfile-jvm", "..")
}

tasks.register<Exec>("buildImagePostgres") {
    commandLine(
        dockerExe,
        "build",
        "-t",
        dockerImagePostgres,
        "-f",
        "../docker/Dockerfile-postgres",
        "..",
    )
}

tasks.register<Exec>("buildImageUbuntu") {
    commandLine(dockerExe, "build", "-t", dockerImageUbuntu, "-f", "../docker/Dockerfile-ubuntu", "..")
}

tasks.register("buildImageAll") {
    dependsOn("buildImageJvm")
    dependsOn("buildImagePostgres")
    dependsOn("buildImageUbuntu")
}

tasks.register<Exec>("allUp") {
    commandLine(dockerExe, "compose", "-f", "../docker-compose.yml", "up", "--force-recreate", "-d")
}

tasks.register<Exec>("allDown") {
    commandLine(dockerExe, "compose", "-f", "../docker-compose.yml", "down")
}
