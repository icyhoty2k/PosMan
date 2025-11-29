import org.gradle.jvm.toolchain.internal.AsdfInstallationSupplier

plugins {
    kotlin("jvm") version "2.2.21"  // Enables Kotlin + Java compilation for buildSrc
}

repositories {
    mavenCentral()

}
