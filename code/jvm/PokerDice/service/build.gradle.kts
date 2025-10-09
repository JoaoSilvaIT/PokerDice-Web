plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":domain"))
    implementation(project(":repo"))

    // To get password encode
    api("org.springframework.security:spring-security-core:6.5.5")

    implementation("org.springframework.boot:spring-boot-starter-web:3.5.6")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
