import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //2.0.21
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    `java-library`
    `maven-publish`
}

group = "io.github.lowley"
version = "1.0.2"

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
    //1.7.3
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    ////////////////////
    // pour Flow<xxx> //
    ////////////////////
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    /////////////////////////////////////////
    // programmation fonctionnelle: Either //
    /////////////////////////////////////////
    implementation("io.arrow-kt:arrow-core:1.2.4")

    //////////////////////////////
    // injections de dépendance //
    //////////////////////////////
    implementation("io.insert-koin:koin-core:4.0.0")

    ///////////////////////////////////////
    // parsing du HTML des textes de log //
    ///////////////////////////////////////
    implementation("org.jsoup:jsoup:1.21.2")

    ////////////////
    // reflection //
    ////////////////
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.21")

    ///////////////////
    // state machine //
    ///////////////////
    val KStateTag = "0.35.0"
    implementation("io.github.nsk90:kstatemachine:$KStateTag")
    implementation("io.github.nsk90:kstatemachine-coroutines:$KStateTag")
    implementation("io.github.nsk90:kstatemachine-serialization:$KStateTag")

}

configurations.all {
    resolutionStrategy {
        val force = force("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.21")
        force("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("WriterAPI") {
            from(components["java"])
            artifactId = "WriterAPI"
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.set(listOf(
        "-Xnested-type-aliases"
    ))
}