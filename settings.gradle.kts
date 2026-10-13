plugins {
    // Downloads a matching JDK automatically if the toolchain declared in build.gradle.kts is absent.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "normaliser"
