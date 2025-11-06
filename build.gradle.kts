plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "1.9.24"
    `java-library`
    `maven-publish`
}

group = "io.github.lowley"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.10.1")

    ////////////////////////
    // pour @Serializable //
    ////////////////////////
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    ////////////////////
    // pour Flow<xxx> //
    ////////////////////
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    /////////////////////////////////////////
    // programmation fonctionnelle: Either //
    /////////////////////////////////////////
    implementation("io.arrow-kt:arrow-core:1.2.4")


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