
plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
}

group = "locutus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven( url = "https://jitpack.io/")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:0.20.0")

    implementation("org.bouncycastle:bcprov-jdk15on:1.65.01")

    implementation("com.google.guava:guava:29.0-jre")
    implementation("com.sksamuel.avro4k:avro4k-core:0.30.0")

    testImplementation("io.kotest:kotest-runner-console-jvm:4.1.1") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.1.1") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:4.1.1") // for kotest property test

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

