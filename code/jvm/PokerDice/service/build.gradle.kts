plugins {
    kotlin("jvm") version "1.9.25"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":domain"))
    implementation(project(":repo"))
}

tasks.test {
    useJUnitPlatform()
}