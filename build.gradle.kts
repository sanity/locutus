plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    id("idea")
    `java-library`
    id("com.github.ben-manes.versions") version "0.33.0"
}

group = "locutus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io/")
    jcenter()
}

tasks.withType<Test> {
    useJUnitPlatform()
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")

    implementation("org.bouncycastle:bcprov-jdk15on:1.66")
    implementation("com.google.guava:guava:30.0-jre")

    implementation("com.github.kwebio:kweb-core:0.7.33")

    implementation("org.mapdb:mapdb:3.0.8")

    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")

    implementation("org.koin:koin-core:2.1.6")

    implementation("com.github.sanity:pairAdjacentViolators:1.4.16")

    implementation("org.bitcoinj:bitcoinj-core:0.15.10")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.3.0")
    testImplementation("io.kotest:kotest-property-jvm:4.3.0")

    testImplementation("org.koin:koin-test:2.1.6")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
