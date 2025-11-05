plugins {
    kotlin("jvm") version "2.2.20"
    `java-library`
    `maven-publish`
}

group = "io.github.lowley"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
}

publishing {
    publications {
        create<MavenPublication>("WriterAPI") {
            from(components["java"])
            artifactId = "WriterAPI"
        }
    }
}