plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":service"))
    implementation("org.springframework.boot:spring-boot-starter-web:3.5.6")

    // To get password encode
    api("org.springframework.security:spring-security-core:6.5.5")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}