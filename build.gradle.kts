plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.pv.entrypoints)
    alias(libs.plugins.pv.kotlin.relocate)
    alias(libs.plugins.pv.java.templates)
    alias(libs.plugins.runpaper)
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    compileOnly(libs.plasmovoice)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.kotlinx.coroutines.jdk8)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://repo.plasmoverse.com/releases")
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
    }

    runServer {
        minecraftVersion("1.21.7")

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-2.1.5")
        }
    }
}
