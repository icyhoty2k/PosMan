// Root Project Name
rootProject.name = "PosMan"

// ------------------------------------------------------------
// 📦 Centralized Dependency Resolution and Repositories
// ------------------------------------------------------------
// This block ensures all subprojects use the same repositories
// and configuration for dependency resolution.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Maven Central is the primary repository for most Java/JVM dependencies (e.g., Spring, JUnit, Apache Commons).
        mavenCentral()

        // Google's Maven repository, often required for Android-related libraries or some Google-maintained artifacts.
        // It's good practice to include it if you use any Google APIs or tools.
        google()

        // JCenter is deprecated, but it's important to include it if your project
        // has legacy dependencies that haven't migrated to Maven Central.
        // For a new JavaFX project, you likely won't need it, but it's a common fallback.
        // jcenter() // (Optional: Omit if not using legacy libraries)

        // Local repository for locally published artifacts (e.g., from `mvn install` or `publishToMavenLocal`).
        // Useful during local development and testing of internal libraries.
        mavenLocal()
    }
}

// ------------------------------------------------------------
// 🔌 Plugin Repository Management
// ------------------------------------------------------------
// Defines where Gradle should look for its own plugins (e.g., 'java', 'application').
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// ------------------------------------------------------------
// ⚡ RAM Disk Build Cache (Global Cache Location)
// ------------------------------------------------------------
buildCache {
    local {
        // Using the RAM Disk on R: for maximum performance, ideal for your powerful systems.
        directory = file("R:/PosMan Build-cache")
        isEnabled = true
    }
}

// ------------------------------------------------------------
// 📚 Subproject Inclusion (Modules)
// ------------------------------------------------------------
include("App")
include("Config")
include("Gui")
include("Logging")
include("Persistence")
include("Resources")
include("Services")
include("Utils")
