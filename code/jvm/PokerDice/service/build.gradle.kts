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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation(project(":repo-jdbi"))

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.springframework:spring-test:6.2.11")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("org.jdbi:jdbi3-core:3.45.1")
    testImplementation("org.jdbi:jdbi3-kotlin:3.45.1")
    testImplementation("org.jdbi:jdbi3-postgres:3.45.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
