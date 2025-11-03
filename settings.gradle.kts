plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "3.17" // Use the latest version
}
rootProject.name = "PosMan"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-use"
        termsOfUseAgree = "yes"
    }
}
