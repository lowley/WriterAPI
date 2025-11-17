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
version = "1.0.4"

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "periscope"
            pom { name.set("périscope") }
        }
    }
    repositories { mavenLocal() }
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.11.0")

    ////////////////////////
    // pour @Serializable //
    ////////////////////////
    //1.7.3
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

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
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.21"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    ///////////////////
    // state machine //
    ///////////////////
    val KStateTag = "0.35.0"
    implementation("io.github.nsk90:kstatemachine:$KStateTag")
    implementation("io.github.nsk90:kstatemachine-coroutines:$KStateTag")
    implementation("io.github.nsk90:kstatemachine-serialization:$KStateTag")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

//val compileKotlin: KotlinCompile by tasks
//compileKotlin.compilerOptions {
//    jvmTarget.set(JvmTarget.JVM_17)
//    freeCompilerArgs.set(listOf(
//        "-Xnested-type-aliases"
//    ))
//}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}
