plugins {
    kotlin("jvm") version "1.9.25"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.jdbi:jdbi3-kotlin:3.37.1")
    implementation("org.jdbi:jdbi3-postgres:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring context for @Component annotation
    implementation("org.springframework:spring-context:6.2.1")

    // Missing critical dependencies
    implementation(project(":domain"))
    implementation(project(":repo"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}