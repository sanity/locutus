plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    id("idea")
    `java-library`
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.9")

    implementation("org.bouncycastle:bcprov-jdk15on:1.65.01")
    implementation("com.google.guava:guava:29.0-jre")
    implementation("io.github.microutils:kotlin-logging:1.8.3")

    implementation("com.github.kwebio:kweb-core:0.7.22")

    implementation("org.mapdb:mapdb:3.0.8")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.2.3")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.2.3")
    testImplementation("io.kotest:kotest-property-jvm:4.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")

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
