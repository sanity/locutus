plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0"
    id("idea")
    `java-library`
}

group = "locutus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io/")
    jcenter()

    // For kotest snapshot
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation("org.bouncycastle:bcprov-jdk15on:1.65.01")

    implementation("com.google.guava:guava:29.0-jre")
    // implementation("com.sksamuel.avro4k:avro4k-core:0.30.0")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.2.0.502-SNAPSHOT")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.2.0.502-SNAPSHOT")
    testImplementation("io.kotest:kotest-property-jvm:4.2.0.502-SNAPSHOT")

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}


idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
